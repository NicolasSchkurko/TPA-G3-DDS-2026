package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/donaciones")
public class DonacionesViewController {

    private final DonacionesClient donacionesClient;

    public DonacionesViewController(DonacionesClient donacionesClient) {
        this.donacionesClient = donacionesClient;
    }

    @GetMapping
    public String listarDonaciones(Model model) {
        var donaciones = donacionesClient.obtenerTodasLasDonaciones();
        model.addAttribute("donaciones", donaciones);
        return "donaciones/listar";
    }

    @GetMapping("/{id}")
    public String detalleDonacion(@PathVariable UUID id, Model model) {
        DonacionDTO donacion = donacionesClient.obtenerDonacionPorId(id);
        if (donacion == null) {
            model.addAttribute("error", "No se encontró la donación solicitada.");
            return "redirect:/donaciones";
        }
        model.addAttribute("donacion", donacion);
        return "donaciones/detalle";
    }

    @GetMapping("/donantes")
    public String listarDonantes(Model model) {
        var donantes = donacionesClient.obtenerDonantes();
        model.addAttribute("donantes", donantes);
        return "donaciones/donantes";
    }
}
