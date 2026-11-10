package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
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
import java.util.Map;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final DonacionesClient donacionesClient;
    private final KeycloakRegistrationClient keycloakRegistrationClient;

    @Value("${servicio.donaciones.url}")
    private String donacionesUrl;

    @Value("${servicio.incentivos.url}")
    private String incentivosUrl;

    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    public HomeController(DonacionesClient donacionesClient, KeycloakRegistrationClient keycloakRegistrationClient) {
        this.donacionesClient = donacionesClient;
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
    public String register(@RequestParam Map<String, String> form, Model model) {
        String userId = null;
        Map<String, String> datosFormulario = new LinkedHashMap<>(form);
        datosFormulario.remove("password");
        model.addAttribute("formularioRegistro", datosFormulario);
        try {
            String role = rolParaTipoRegistro(form.get("tipoRegistro"));
            String username = form.get("username");
            String password = form.get("password");
            if (username == null || username.trim().length() < 3 || password == null || password.length() < 8) {
                throw new IllegalArgumentException("Ingresá un usuario de al menos 3 caracteres y una contraseña de al menos 8 caracteres.");
            }
            if ("DONANTE".equals(form.get("tipoRegistro"))
                    && (!"EMAIL".equals(form.get("tipoContacto"))
                    || form.get("valorContacto") == null
                    || form.get("valorContacto").isBlank())) {
                throw new IllegalArgumentException("Ingresá un correo electrónico para vincular tu perfil de donante con la cuenta.");
            }
            String nombre = "HUMANA".equals(form.get("tipoPersona"))
                    ? form.get("nombre") : form.get("razonSocial");
            String apellido = "HUMANA".equals(form.get("tipoPersona")) ? form.get("apellido") : null;
            String correo = "EMAIL".equals(form.get("tipoContacto")) ? form.get("valorContacto") : null;
            userId = keycloakRegistrationClient.crearUsuario(username.trim(), password, role,
                    nombre, apellido, correo);
            try {
                donacionesClient.registrarUsuario(form.get("tipoRegistro"), form);
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

    private static String rolParaTipoRegistro(String tipoRegistro) {
        if (tipoRegistro == null) throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
        return switch (tipoRegistro) {
            case "DONANTE" -> "DONANTE";
            case "ENTIDAD" -> "ENTIDAD";
            default -> throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
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
