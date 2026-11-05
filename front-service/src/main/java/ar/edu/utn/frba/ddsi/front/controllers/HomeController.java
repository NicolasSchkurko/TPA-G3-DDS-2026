package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @Value("${servicio.mapa.tiles-url:}")
    private String mapTilesUrl;

    @Value("${servicio.mapa.attribution:}")
    private String mapAttribution;

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
    public String landing(Model model) {
        // Si no se configura un proveedor de tiles, el JS usa Esri por defecto.
        model.addAttribute("mapTilesUrl", cleanConfigValue(mapTilesUrl));
        model.addAttribute("mapAttribution", cleanConfigValue(mapAttribution));
        return "landing";
    }

    /**
     * Los .env suelen escribir los valores entre comillas. Spring los lee como
     * properties, así que las sacamos acá antes de pasarlos al template.
     */
    private static String cleanConfigValue(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }
        return cleaned;
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
