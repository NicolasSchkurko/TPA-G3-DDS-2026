package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {
    private final DonacionesClient donacionesClient;

    public ProfileController(DonacionesClient donacionesClient) {
        this.donacionesClient = donacionesClient;
    }

    @GetMapping("/donante")
    public String donante(Authentication authentication, Model model) {
        return perfil(authentication, model, "Espacio donante", "DONANTE",
                "Desde acá vas a poder consultar tus donaciones y gestionar tu participación.");
    }

    @GetMapping("/donante/mapa")
    public String mapaDonante(Model model) {
        model.addAttribute("entidades", donacionesClient.obtenerEntidades());
        return "donante/mapa";
    }

    @GetMapping("/chofer")
    public String chofer(Authentication authentication, Model model) {
        return perfil(authentication, model, "Espacio chofer", "CHOFER",
                "Desde acá vas a poder consultar tus rutas y actualizar el estado de tus entregas.");
    }

    @GetMapping("/entidad")
    public String entidad(Authentication authentication, Model model) {
        return perfil(authentication, model, "Espacio entidad beneficiaria", "ENTIDAD",
                "Desde acá vas a poder consultar y actualizar las necesidades de tu entidad.");
    }

    @GetMapping("/sin-acceso")
    public String sinAcceso() {
        return "sin-acceso";
    }

    private String perfil(Authentication authentication, Model model, String titulo, String rol, String descripcion) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("rol", rol);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("usuario", authentication.getName());
        return "perfil";
    }
}
