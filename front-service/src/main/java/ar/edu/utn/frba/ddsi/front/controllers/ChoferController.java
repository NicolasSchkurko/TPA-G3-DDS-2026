package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.dto.ChoferDashboardDTO;
import ar.edu.utn.frba.ddsi.front.services.ChoferPortalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/chofer")
public class ChoferController {

    private static final Logger log = LoggerFactory.getLogger(ChoferController.class);

    private final ChoferPortalService choferPortalService;

    public ChoferController(ChoferPortalService choferPortalService) {
        this.choferPortalService = choferPortalService;
    }

    @GetMapping
    public String dashboard(Authentication authentication,
                            @RequestParam(name = "historialPage", defaultValue = "0") int historialPage,
                            Model model) {
        ChoferDashboardDTO dashboard = choferPortalService.construir(authentication, historialPage);
        model.addAttribute("dashboard", dashboard);
        return "chofer/dashboard";
    }

    @PostMapping("/ruta/iniciar")
    public String iniciarRuta(Authentication authentication, RedirectAttributes flash) {
        try {
            choferPortalService.iniciarRuta(authentication);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "Ruta iniciada. Ya podés marcar las entregas.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", e.getMessage());
        } catch (Exception e) {
            log.warn("No se pudo iniciar la ruta del chofer: {}", e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo iniciar la ruta. Verificá que tengas una ruta programada.");
        }
        return "redirect:/chofer";
    }

    @PostMapping("/ruta/terminar")
    public String terminarRuta(Authentication authentication, RedirectAttributes flash) {
        try {
            choferPortalService.terminarRuta(authentication);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "Ruta finalizada. Las entregas confirmadas quedaron registradas.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", e.getMessage());
        } catch (Exception e) {
            log.warn("No se pudo terminar la ruta del chofer: {}", e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo terminar la ruta. Intentá de nuevo.");
        }
        return "redirect:/chofer";
    }

    @PostMapping("/entregas/{id}/entregar")
    public String marcarEntregada(@PathVariable UUID id,
                                  @RequestParam("fotoBase64") String fotoBase64,
                                  @RequestParam(name = "observacion", required = false) String observacion,
                                  Authentication authentication,
                                  RedirectAttributes flash) {
        try {
            choferPortalService.confirmarEntrega(authentication, id, fotoBase64, observacion);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "Entrega confirmada. Si la parada quedó completa, seguí con la próxima.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", e.getMessage());
        } catch (Exception e) {
            log.warn("No se pudo confirmar la entrega {}: {}", id, e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo confirmar la entrega. Intentá de nuevo.");
        }
        return "redirect:/chofer";
    }
}
