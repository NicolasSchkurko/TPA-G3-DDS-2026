package ar.edu.utn.frba.ddsi.front.controllers;


import ar.edu.utn.frba.ddsi.front.clients.LogisticaClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Controller
public class AdminController {

    private final LogisticaClient logisticaClient;

    public AdminController(LogisticaClient logisticaClient) {
        this.logisticaClient = logisticaClient;
    }

    @GetMapping("/admin")
    public String mostrarAdmin(
            @AuthenticationPrincipal OidcUser usuario,
            Model model) {

        model.addAttribute("nombre", usuario.getGivenName());
        model.addAttribute("apellido", usuario.getFamilyName());
        model.addAttribute("username", usuario.getPreferredUsername());

        boolean esAdministrador = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("rol", esAdministrador ? "Administrador" : "Usuario");

        return "admin/admin-landing";
    }

    @GetMapping("/admin/donaciones")
    public String mostrarDonaciones() {
        return "admin/donaciones";
    }

    @GetMapping("/admin/entidades")
    public String mostrarEntidades() {
        return "admin/entidades";
    }

    @GetMapping("/landing")
    public String mostrarlanding() {
        return "/landing";
    }

    @GetMapping("/camiones")
    public String listarcamiones(Model model) {
        var camiones = logisticaClient.obtenerTodosLoscamiones();
        model.addAttribute("camiones", camiones);
        return "/camiones";
    }
}
