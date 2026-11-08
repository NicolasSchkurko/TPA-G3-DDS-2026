package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

@Controller
public class HomeController {

    private final DonacionesClient donacionesClient;

    @Value("${servicio.donaciones.url}")
    private String donacionesUrl;

    @Value("${servicio.incentivos.url}")
    private String incentivosUrl;

    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    public HomeController(DonacionesClient donacionesClient) {
        this.donacionesClient = donacionesClient;
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

    /**
     * Landing pública: no depende de los microservicios, así que puede mostrarse
     * aunque el backend no esté levantado.
     */
    @GetMapping({"/", "/landing"})
    public String landing(Authentication authentication, Model model) {
        boolean esDonante = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"))
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
