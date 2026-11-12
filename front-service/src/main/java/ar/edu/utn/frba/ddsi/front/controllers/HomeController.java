package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import ar.edu.utn.frba.ddsi.front.dto.DireccionBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDestacadaDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDestacadaMapaDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final DonacionesClient donacionesClient;
    private final IncentivosClient incentivosClient;
    private final KeycloakRegistrationClient keycloakRegistrationClient;

    @Value("${servicio.donaciones.url}")
    private String donacionesUrl;

    @Value("${servicio.incentivos.url}")
    private String incentivosUrl;

    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    public HomeController(DonacionesClient donacionesClient,
                          IncentivosClient incentivosClient,
                          KeycloakRegistrationClient keycloakRegistrationClient) {
        this.donacionesClient = donacionesClient;
        this.incentivosClient = incentivosClient;
        this.keycloakRegistrationClient = keycloakRegistrationClient;
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
            if (username == null || username.trim().length() < 3 || password == null || password.length() < 8) {
                throw new IllegalArgumentException("Ingresá un usuario de al menos 3 caracteres y una contraseña de al menos 8 caracteres.");
            }
            String tipoRegistro = form.get("tipoRegistro");
            String correoFormulario = "ENTIDAD".equals(tipoRegistro)
                    ? form.get("correoElectronico") : form.get("valorContacto");
            if (("DONANTE".equals(tipoRegistro) || "ENTIDAD".equals(tipoRegistro))
                    && (correoFormulario == null || correoFormulario.isBlank()
                    || ("DONANTE".equals(tipoRegistro) && !"EMAIL".equals(form.get("tipoContacto"))))) {
                throw new IllegalArgumentException("Ingresá un correo electrónico para vincular tu perfil con la cuenta.");
            }
            String nombre = "HUMANA".equals(form.get("tipoPersona"))
                    ? form.get("nombre") : form.get("razonSocial");
            String apellido = "HUMANA".equals(form.get("tipoPersona")) ? form.get("apellido") : null;
            String correo = "ENTIDAD".equals(tipoRegistro)
                    ? form.get("correoElectronico")
                    : "EMAIL".equals(form.get("tipoContacto")) ? form.get("valorContacto") : null;
            userId = keycloakRegistrationClient.crearUsuario(username.trim(), password, role,
                    nombre, apellido, correo);
            try {
                donacionesClient.registrarUsuario(form.get("tipoRegistro"), form, userId);
            } catch (RestClientResponseException e) {
                eliminarCuentaSiCorresponde(userId);
                userId = null;
                log.warn("El servicio de perfiles rechazó el registro con estado {}", e.getStatusCode());
                model.addAttribute("error", mensajeErrorPerfil(e));
                return "register";
            }
            model.addAttribute("msg", "Tu cuenta quedó creada. Ya podés ingresar con tu usuario y contraseña.");
        } catch (IllegalArgumentException e) {
            eliminarCuentaSiCorresponde(userId);
            model.addAttribute("error", e.getMessage());
        } catch (RestClientResponseException e) {
            eliminarCuentaSiCorresponde(userId);
            log.warn("Keycloak rechazó el registro con estado {}", e.getStatusCode());
            model.addAttribute("error", e.getStatusCode().value() == 409
                    ? "Ya existe una cuenta con ese usuario o correo. Revisalos e intentá nuevamente."
                    : "Keycloak no pudo crear la cuenta. Revisá el usuario, el correo y la contraseña e intentá nuevamente.");
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

    private void eliminarCuentaSiCorresponde(String userId) {
        if (userId == null) return;
        try {
            keycloakRegistrationClient.eliminarUsuario(userId);
        } catch (Exception e) {
            log.error("No se pudo revertir el alta de la cuenta de Keycloak {}", userId, e);
        }
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
        List<DonacionDestacadaDTO> destacadas = incentivosClient.obtenerDonacionesDestacadas();
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
        DireccionBeneficiariaDTO direccion = entidad != null ? entidad.getDireccion() : null;
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

    private String nombreUbicacion(DireccionBeneficiariaDTO direccion) {
        if (direccion.getCiudad() != null && !direccion.getCiudad().isBlank()) {
            return direccion.getCiudad();
        }
        return direccion.getProvincia() != null && !direccion.getProvincia().isBlank()
                ? direccion.getProvincia()
                : direccion.getPais();
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        var donaciones = donacionesClient.obtenerTodasLasDonaciones();
        var donantes = donacionesClient.obtenerDonantes();

        model.addAttribute("totalDonaciones", donaciones.size());
        model.addAttribute("totalDonantes", donantes.size());
        model.addAttribute("donacionesUrl", donacionesUrl);
        model.addAttribute("incentivosUrl", incentivosUrl);
        model.addAttribute("notificacionesUrl", notificacionesUrl);
        model.addAttribute("logisticasUrl", logisticasUrl);

        return "panel";
    }
}
