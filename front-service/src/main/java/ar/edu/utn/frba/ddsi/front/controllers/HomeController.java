package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final DonacionesClient donacionesClient;
    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    @Value("${servicio.donaciones.url}")
    private String donacionesUrl;

    @Value("${servicio.incentivos.url}")
    private String incentivosUrl;

    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    public HomeController(DonacionesClient donacionesClient,
                          UserDetailsManager userDetailsManager,
                          PasswordEncoder passwordEncoder) {
        this.donacionesClient = donacionesClient;
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
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
        try {
            String username = form.get("username") == null ? "" : form.get("username").trim();
            String password = form.get("password");
            if (username.isBlank()) {
                throw new IllegalArgumentException("Ingresá un nombre de usuario.");
            }
            if (password == null || password.length() < 8) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
            }
            if (userDetailsManager.userExists(username)) {
                throw new IllegalArgumentException("Ese nombre de usuario ya está registrado.");
            }

            Map<String, String> datosRegistro = new LinkedHashMap<>(form);
            datosRegistro.remove("username");
            datosRegistro.remove("password");
            donacionesClient.registrarUsuario(form.get("tipoRegistro"), datosRegistro);
            userDetailsManager.createUser(User.withUsername(username)
                    .password(passwordEncoder.encode(password))
                    .roles("USER")
                    .build());
            model.addAttribute("msg", "Cuenta creada. Ya podés iniciar sesión con tu usuario y contraseña.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (RestClientResponseException e) {
            log.warn("Donaciones rechazó el registro con estado {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            String error = e.getResponseBodyAsString();
            model.addAttribute("error", error.isBlank()
                    ? "El servicio de Donaciones no pudo completar el registro."
                    : error);
        } catch (ResourceAccessException e) {
            log.error("No se pudo conectar con el servicio de Donaciones para registrar el usuario: {}", e.getMessage());
            model.addAttribute("error", "No se pudo conectar con el servicio de Donaciones. Intentá nuevamente más tarde.");
        }
        return "register";
    }

    /**
     * Landing pública: no depende de los microservicios, así que puede mostrarse
     * aunque el backend no esté levantado.
     */
    @GetMapping({"/", "/landing"})
    public String landing() {
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
