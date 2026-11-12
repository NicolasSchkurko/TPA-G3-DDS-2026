package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import ar.edu.utn.frba.ddsi.front.config.PoliticaDeReclamo;
import ar.edu.utn.frba.ddsi.front.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDestacadaDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDestacadaMapaDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private static final String CONFLICTO_DE_PERFIL =
            "Ese perfil ya está vinculado a otra cuenta. Iniciá sesión con la cuenta original "
                    + "o contactá a un administrador.";

    private final DonacionesClient donacionesClient;
    private final IncentivosClient incentivosClient;
    private final KeycloakRegistrationClient keycloakRegistrationClient;
    private final PoliticaDeReclamo politicaDeReclamo;

    @Autowired
    public HomeController(DonacionesClient donacionesClient,
                          IncentivosClient incentivosClient,
                          KeycloakRegistrationClient keycloakRegistrationClient,
                          PoliticaDeReclamo politicaDeReclamo) {
        this.donacionesClient = donacionesClient;
        this.incentivosClient = incentivosClient;
        this.keycloakRegistrationClient = keycloakRegistrationClient;
        this.politicaDeReclamo = politicaDeReclamo;
    }

    /** Compatibilidad con los stubs de tests (sin landing de destacadas). */
    public HomeController(DonacionesClient donacionesClient,
                          KeycloakRegistrationClient keycloakRegistrationClient,
                          PoliticaDeReclamo politicaDeReclamo) {
        this(donacionesClient, null, keycloakRegistrationClient, politicaDeReclamo);
    }

    /** Compatibilidad con los stubs de tests (verificación de email desactivada). */
    public HomeController(DonacionesClient donacionesClient, KeycloakRegistrationClient keycloakRegistrationClient) {
        this(donacionesClient, null, keycloakRegistrationClient, new PoliticaDeReclamo(false));
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña inválidos.");
        }
        if (logout != null) {
            model.addAttribute("msg", "Has cerrado sesión correctamente.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /** Compatibilidad con tests y llamadas internas sin autenticación. */
    public String register(Map<String, String> form, Model model) {
        return register(form, model, null);
    }

    @PostMapping("/register")
    public String register(@RequestParam Map<String, String> form, Model model,
                           Authentication authentication) {
        String userId = null;
        Map<String, String> datosFormulario = new LinkedHashMap<>(form);
        datosFormulario.remove("password");
        model.addAttribute("formularioRegistro", datosFormulario);
        try {
            String role = rolParaTipoRegistro(form.get("tipoRegistro"), authentication);
            String username = form.get("username");
            String password = form.get("password");
            String email = normalizarEmail(form.get("email"));
            if (username == null || username.trim().length() < 3 || password == null || password.length() < 8) {
                throw new IllegalArgumentException("Ingresá un usuario de al menos 3 caracteres y una contraseña de al menos 8 caracteres.");
            }
            username = username.trim();
            if (email == null) {
                throw new IllegalArgumentException("Ingresá un correo electrónico válido (por ejemplo, nombre@dominio.com).");
            }

            // 1) Pre-chequeo contra Keycloak: si ya existe la cuenta, no creamos nada y derivamos
            //    a "iniciá sesión y agregá el rol" desde la cuenta existente.
            if (keycloakRegistrationClient.existeUsuarioConEmail(email)) {
                model.addAttribute("error", "Ya existe una cuenta con ese correo. Iniciá sesión y agregá el rol "
                        + etiquetaRole(role) + " desde tu cuenta.");
                return "register";
            }
            if (keycloakRegistrationClient.existeUsuarioConUsername(username)) {
                model.addAttribute("error", "Ese nombre de usuario ya está en uso. Elegí otro o iniciá sesión.");
                return "register";
            }

            // 2) Cuenta en Keycloak (si falla acá no se crea ningún perfil).
            String[] nombreApellido = nombreApellidoPara(form, username);
            try {
                userId = keycloakRegistrationClient.crearUsuario(username, password, email, role,
                        nombreApellido[0], nombreApellido[1]);
            } catch (RestClientResponseException e) {
                log.warn("Keycloak rechazó la creación de la cuenta: {}", e.getStatusCode());
                model.addAttribute("error", e.getStatusCode().value() == 409
                        ? "Ya existe una cuenta con ese correo o usuario. Iniciá sesión y agregá el rol desde tu cuenta."
                        : "No pudimos crear tu cuenta. Intentá nuevamente más tarde.");
                return "register";
            }

            // 3) Perfil de dominio. Si el email ya tiene un perfil del mismo rol, se reutiliza en
            //    lugar de duplicarlo; si no, se crea uno nuevo.
            try {
                IdentidadDTO identidadPrevia = donacionesClient.buscarIdentidadPorEmail(email);
                java.util.UUID perfilId = perfilPorRol(identidadPrevia, role);
                if (perfilId == null) {
                    Map<String, String> datos = new LinkedHashMap<>(form);
                    datos.put("email", email);
                    datos.put("username", username);
                    if (userId != null) {
                        datos.put("keycloakSub", userId);
                    }
                    perfilId = donacionesClient.registrarUsuario(form.get("tipoRegistro"), datos, userId);
                } else if (!politicaDeReclamo.puedeReclamar(identidadPrevia.isVinculada(), false)) {
                    // El perfil ya está reclamado por otra cuenta: no se puede apropiar.
                    eliminarCuentaSiCorresponde(userId);
                    model.addAttribute("error", CONFLICTO_DE_PERFIL);
                    return "register";
                } else if (userId != null) {
                    // Perfil huérfano: lo vinculamos con esta cuenta de Keycloak.
                    try {
                        donacionesClient.vincularIdentidad(userId, role, perfilId);
                    } catch (Exception mappingError) {
                        eliminarCuentaSiCorresponde(userId);
                        log.warn("No se pudo vincular el perfil existente de {} ({}): {}", username, role, mappingError.getMessage());
                        model.addAttribute("error", CONFLICTO_DE_PERFIL);
                        return "register";
                    }
                }
            } catch (IllegalArgumentException e) {
                eliminarCuentaSiCorresponde(userId);
                model.addAttribute("error", e.getMessage());
                return "register";
            } catch (RestClientResponseException e) {
                eliminarCuentaSiCorresponde(userId);
                log.warn("Donaciones rechazó el alta con estado {}", e.getStatusCode());
                model.addAttribute("error", mensajeErrorPerfil(e));
                return "register";
            } catch (Exception e) {
                eliminarCuentaSiCorresponde(userId);
                log.error("Error al crear el perfil en Donaciones", e);
                model.addAttribute("error", "No pudimos guardar tu perfil. Intentá nuevamente más tarde.");
                return "register";
            }

            // 4) Sincronizar roles: el solicitado + los que ya estén vinculados al email.
            try {
                Set<String> roles = new LinkedHashSet<>();
                roles.add(role);
                ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO identidad = userId != null
                        ? donacionesClient.buscarIdentidadPorSub(userId) : null;
                if (identidad == null) {
                    // El fallback por email solo puede traer un perfil huérfano:
                    // nunca se heredan roles de un perfil ya reclamado por otra cuenta.
                    IdentidadDTO porEmail = donacionesClient.buscarIdentidadPorEmail(email);
                    if (porEmail != null && politicaDeReclamo.puedeReclamar(porEmail.isVinculada(), false)) {
                        identidad = porEmail;
                    }
                }
                if (identidad != null) {
                    if (identidad.getRoles() != null) {
                        roles.addAll(identidad.getRoles());
                    }
                    if (userId != null) {
                        if (identidad.getEntidadId() != null) {
                            vincularSilencioso(userId, "ENTIDAD", identidad.getEntidadId(), username);
                        }
                        if (identidad.getDonanteId() != null) {
                            vincularSilencioso(userId, "DONANTE", identidad.getDonanteId(), username);
                        }
                        if (identidad.getAdminId() != null) {
                            vincularSilencioso(userId, "ADMIN", identidad.getAdminId(), username);
                        }
                    }
                }
                roles.addAll(keycloakRegistrationClient.rolesDeUsuariosConEmail(email));
                keycloakRegistrationClient.asignarRoles(userId, roles);
            } catch (Exception roleError) {
                // El alta ya quedó hecha: una falla al sincronizar roles no debe tumbar el registro.
                log.warn("No se pudieron sincronizar los roles de {} con el email {}: {}", username, email, roleError.getMessage());
            }
            model.addAttribute("msg", "Tu cuenta quedó creada. Ya podés ingresar con tu usuario y contraseña.");
        } catch (IllegalArgumentException e) {
            eliminarCuentaSiCorresponde(userId);
            model.addAttribute("error", e.getMessage());
        } catch (ResourceAccessException e) {
            eliminarCuentaSiCorresponde(userId);
            log.error("No se pudo conectar con Keycloak o Donaciones durante el registro: {}", e.getMessage());
            model.addAttribute("error", "No se pudo conectar con los servicios de registro. Intentá nuevamente más tarde.");
        } catch (Exception e) {
            eliminarCuentaSiCorresponde(userId);
            log.error("Error al crear cuenta en Keycloak o perfil en Donaciones", e);
            model.addAttribute("error", "No se pudo crear la cuenta. Intentá nuevamente más tarde.");
        }
        return "register";
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static String normalizarEmail(String email) {
        if (email == null) return null;
        String normalizado = email.trim().toLowerCase();
        return EMAIL_PATTERN.matcher(normalizado).matches() ? normalizado : null;
    }

    private static String mensajeErrorPerfil(RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 409) {
            return "Ya existe un perfil con esos datos. Revisá el documento o CUIT e intentá nuevamente.";
        }
        if (exception.getStatusCode().is4xxClientError()) {
            String detalle = exception.getResponseBodyAsString().trim();
            if (!detalle.isEmpty() && detalle.length() <= 300 && !detalle.startsWith("{")) {
                return detalle;
            }
            return "El servicio de perfiles rechazó los datos. Revisá los campos del formulario e intentá nuevamente.";
        }
        return "No se pudo guardar el perfil de donante. Intentá nuevamente más tarde.";
    }

    private static String rolParaTipoRegistro(String tipoRegistro, Authentication authentication) {
        if (tipoRegistro == null) throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
        return switch (tipoRegistro) {
            case "DONANTE" -> "DONANTE";
            case "ENTIDAD" -> "ENTIDAD";
            case "ADMIN" -> {
                if (!esAdmin(authentication)) {
                    throw new IllegalArgumentException(
                            "Solo un administrador puede crear otro administrador.");
                }
                yield "ADMIN";
            }
            default -> throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
        };
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /** Nombre y apellido visibles para Keycloak (el perfil declarativo los exige). */
    private static String[] nombreApellidoPara(Map<String, String> form, String username) {
        String nombre = blankToNull(form.get("nombre"));
        String apellido = blankToNull(form.get("apellido"));
        String razonSocial = blankToNull(form.get("razonSocial"));
        String nombreAMostrar = blankToNull(form.get("nombreAMostrar"));
        if (nombre != null) {
            return new String[]{nombre, apellido != null ? apellido : nombre};
        }
        if (razonSocial != null) {
            return new String[]{razonSocial, razonSocial};
        }
        if (nombreAMostrar != null) {
            return new String[]{nombreAMostrar, nombreAMostrar};
        }
        return new String[]{username, username};
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Perfil de dominio ya vinculado al email para el rol indicado, o null si no existe. */
    private static java.util.UUID perfilPorRol(IdentidadDTO identidad, String rol) {
        if (identidad == null || rol == null) return null;
        return switch (rol) {
            case "DONANTE" -> identidad.getDonanteId();
            case "ENTIDAD" -> identidad.getEntidadId();
            case "ADMIN" -> identidad.getAdminId();
            default -> null;
        };
    }

    private static String etiquetaRole(String rol) {
        return switch (rol) {
            case "DONANTE" -> "Donante";
            case "ENTIDAD" -> "Entidad beneficiaria";
            case "ADMIN" -> "Administración";
            default -> rol;
        };
    }

    /** Vincula el perfil con la cuenta de Keycloak sin romper el alta si falla. */
    private void vincularSilencioso(String userId, String rol, java.util.UUID perfilId, String username) {
        try {
            donacionesClient.vincularIdentidad(userId, rol, perfilId);
        } catch (Exception e) {
            log.warn("No se pudo vincular el perfil de {} ({}): {}", username, rol, e.getMessage());
        }
    }

    private void eliminarCuentaSiCorresponde(String userId) {
        if (userId == null) return;
        try {
            keycloakRegistrationClient.eliminarUsuario(userId);
        } catch (Exception e) {
            log.error("No se pudo revertir el alta de la cuenta de Keycloak {}", userId, e);
        }
    }

    /** Términos y condiciones y política de privacidad. Página pública. */
    @GetMapping("/terminos")
    public String terminos() {
        return "terminos";
    }

    /**
     * Landing pública: si incentivos no responde, la lista de destacadas queda vacía y el
     * landing se sigue mostrando.
     */
    @GetMapping({"/", "/landing"})
    public String landing(Authentication authentication, Model model) {
        boolean esDonante = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DONANTE"))
                && !esAdmin(authentication);
        List<DonacionDestacadaDTO> destacadas = incentivosClient != null
                ? incentivosClient.obtenerDonacionesDestacadas()
                : Collections.emptyList();
        List<EntidadBeneficiariaDTO> entidades = donacionesClient.obtenerEntidades();
        List<DonacionDTO> donaciones = donacionesClient.obtenerTodasLasDonaciones();
        model.addAttribute("esDonante", esDonante);
        model.addAttribute("destacadas", destacadas);
        model.addAttribute("destacadasMapa", enriquecerDestacadasConUbicacion(destacadas, entidades));
        model.addAttribute("entregadasMapa",
                enriquecerEntregadasConUbicacion(destacadas, donaciones, entidades));
        model.addAttribute("entidades", esDonante ? entidades : Collections.emptyList());
        return "landing";
    }

    private List<DonacionDestacadaMapaDTO> enriquecerDestacadasConUbicacion(
            List<DonacionDestacadaDTO> destacadas,
            List<EntidadBeneficiariaDTO> entidades) {
        Map<String, EntidadBeneficiariaDTO> entidadesPorNombre = indexarEntidades(entidades);

        return destacadas.stream()
                .map(donacion -> crearDonacionMapa(
                        donacion.idDonacion(),
                        donacion.fechaEntrega(),
                        donacion.cantidadBienes(),
                        donacion.categoria(),
                        donacion.subCategoria(),
                        donacion.entidadBeneficiaria(),
                        entidadesPorNombre))
                .toList();
    }

    private List<DonacionDestacadaMapaDTO> enriquecerEntregadasConUbicacion(
            List<DonacionDestacadaDTO> destacadas,
            List<DonacionDTO> donaciones,
            List<EntidadBeneficiariaDTO> entidades) {
        Set<String> idsDestacados = destacadas.stream()
                .filter(donacion -> donacion.idDonacion() != null)
                .map(donacion -> donacion.idDonacion().toString())
                .collect(Collectors.toSet());
        Map<String, EntidadBeneficiariaDTO> entidadesPorNombre = indexarEntidades(entidades);

        return donaciones.stream()
                .filter(donacion -> "ENTREGADO".equalsIgnoreCase(donacion.getEstado()))
                .filter(donacion -> donacion.getId() != null
                        && !idsDestacados.contains(donacion.getId().toString()))
                .map(donacion -> crearDonacionMapa(
                        donacion.getId(),
                        donacion.getFechaEntrega() != null
                                ? donacion.getFechaEntrega().atStartOfDay()
                                : null,
                        donacion.getCantidadTotalBienes(),
                        donacion.getCategoriaBienName(),
                        donacion.getSubcategoriaName(),
                        donacion.getEntidadBeneficiaria(),
                        entidadesPorNombre))
                .toList();
    }

    private Map<String, EntidadBeneficiariaDTO> indexarEntidades(List<EntidadBeneficiariaDTO> entidades) {
        return entidades.stream()
                .filter(entidad -> entidad.getRazonSocial() != null)
                .collect(Collectors.toMap(
                        entidad -> entidad.getRazonSocial().trim().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (primera, ignorada) -> primera,
                        LinkedHashMap::new));
    }

    private DonacionDestacadaMapaDTO crearDonacionMapa(
            UUID id,
            LocalDateTime fechaEntrega,
            Integer cantidad,
            String categoria,
            String subcategoria,
            String nombreEntidad,
            Map<String, EntidadBeneficiariaDTO> entidadesPorNombre) {
        EntidadBeneficiariaDTO entidad = nombreEntidad == null
                ? null
                : entidadesPorNombre.get(nombreEntidad.trim().toLowerCase(Locale.ROOT));
        DireccionDTO direccion = entidad != null ? entidad.getDireccion() : null;
        return new DonacionDestacadaMapaDTO(
                id,
                fechaEntrega,
                cantidad,
                categoria,
                subcategoria,
                nombreEntidad,
                direccion != null ? direccion.getLatitud() : null,
                direccion != null ? direccion.getLongitud() : null,
                direccion != null ? nombreUbicacion(direccion) : null);
    }

    private String nombreUbicacion(DireccionDTO direccion) {
        if (direccion.getCiudad() != null && !direccion.getCiudad().isBlank()) {
            return direccion.getCiudad();
        }
        return direccion.getProvincia() != null && !direccion.getProvincia().isBlank()
                ? direccion.getProvincia()
                : direccion.getPais();
    }

    @GetMapping("/panel")
    public String panel() {
        return "redirect:/admin";
    }
}
