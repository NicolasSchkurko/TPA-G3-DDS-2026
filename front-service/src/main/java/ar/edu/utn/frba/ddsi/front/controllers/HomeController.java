package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO;
import ar.edu.utn.frba.ddsi.front.services.IdentidadMappingStore;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final DonacionesClient donacionesClient;
    private final KeycloakRegistrationClient keycloakRegistrationClient;
    private final IdentidadMappingStore identidadMappingStore;

    @Value("${servicio.donaciones.url}")
    private String donacionesUrl;

    @Value("${servicio.incentivos.url}")
    private String incentivosUrl;

    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    public HomeController(DonacionesClient donacionesClient, KeycloakRegistrationClient keycloakRegistrationClient,
                          IdentidadMappingStore identidadMappingStore) {
        this.donacionesClient = donacionesClient;
        this.keycloakRegistrationClient = keycloakRegistrationClient;
        this.identidadMappingStore = identidadMappingStore;
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
    public String register(@RequestParam Map<String, String> form, Model model) {
        String userId = null;
        try {
            String role = rolParaTipoRegistro(form.get("tipoRegistro"));
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
                    perfilId = donacionesClient.registrarUsuario(form.get("tipoRegistro"), datos);
                }
                if (perfilId != null) {
                    try {
                        identidadMappingStore.guardar(username, role, perfilId);
                    } catch (Exception mappingError) {
                        // El alta ya quedó persistida; el vínculo se puede rehacer desde "Vincular mi perfil".
                        log.warn("No se pudo guardar el vínculo de identidad para {} ({}): {}", username, role, mappingError.getMessage());
                    }
                }
            } catch (IllegalArgumentException e) {
                eliminarCuentaSiCorresponde(userId);
                model.addAttribute("error", e.getMessage());
                return "register";
            } catch (RestClientResponseException e) {
                eliminarCuentaSiCorresponde(userId);
                log.warn("Donaciones rechazó el alta con estado {}", e.getStatusCode());
                model.addAttribute("error", e.getStatusCode().value() == 409
                        ? "Ya existe un perfil con esos datos (documento, CUIT o email). Si es tu cuenta, iniciá sesión y agregá el rol desde ella."
                        : "No se pudo crear el perfil del rol. Revisá los datos e intentá nuevamente.");
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
                ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO identidad = donacionesClient.buscarIdentidadPorEmail(email);
                if (identidad != null) {
                    if (identidad.getRoles() != null) {
                        roles.addAll(identidad.getRoles());
                    }
                    if (identidad.getEntidadId() != null) {
                        identidadMappingStore.guardar(username, "ENTIDAD", identidad.getEntidadId());
                    }
                    if (identidad.getDonanteId() != null) {
                        identidadMappingStore.guardar(username, "DONANTE", identidad.getDonanteId());
                    }
                    if (identidad.getAdminId() != null) {
                        identidadMappingStore.guardar(username, "ADMIN", identidad.getAdminId());
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

    private static String rolParaTipoRegistro(String tipoRegistro) {
        if (tipoRegistro == null) throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
        return switch (tipoRegistro) {
            case "DONANTE" -> "DONANTE";
            case "ENTIDAD" -> "ENTIDAD";
            default -> throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
        };
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

    private void eliminarCuentaSiCorresponde(String userId) {
        if (userId == null) return;
        try {
            keycloakRegistrationClient.eliminarUsuario(userId);
        } catch (Exception e) {
            log.error("No se pudo revertir el alta de la cuenta de Keycloak {}", userId, e);
        }
    }

    /**
     * Landing pública: no depende de los microservicios, así que puede mostrarse
     * aunque el backend no esté levantado.
     */
    @GetMapping({"/", "/landing"})
    public String landing(Authentication authentication, Model model) {
        boolean esDonante = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DONANTE"))
                && authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("esDonante", esDonante);
        model.addAttribute("entidades", esDonante
                ? donacionesClient.obtenerEntidades()
                : Collections.emptyList());
        return "landing";
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
