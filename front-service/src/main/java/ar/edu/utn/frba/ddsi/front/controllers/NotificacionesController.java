package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.services.NotificacionesDerivadasService;
import ar.edu.utn.frba.ddsi.front.services.NotificacionesUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Bandeja genérica de notificaciones del sistema para cualquier usuario autenticado.
 * La entidad conserva su bandeja enriquecida en /entidad/notificaciones.
 */
@Controller
@RequestMapping("/notificaciones")
public class NotificacionesController {

    private final NotificacionesUsuarioService notificacionesUsuarioService;

    public NotificacionesController(NotificacionesUsuarioService notificacionesUsuarioService) {
        this.notificacionesUsuarioService = notificacionesUsuarioService;
    }

    @GetMapping
    public String notificaciones(@RequestParam(value = "filtro", defaultValue = "todas") String filtro,
                                 Authentication authentication, HttpServletRequest request,
                                 HttpSession session, Model model) {
        String filtroNormalizado = "noLeidas".equalsIgnoreCase(filtro) ? "noLeidas" : "todas";
        List<NotificacionesDerivadasService.Notificacion> sistema =
                notificacionesUsuarioService.notificaciones(request, authentication, session);
        long sinLeer = sistema.stream().filter(n -> !n.leida()).count();
        List<NotificacionesDerivadasService.Notificacion> visibles = "noLeidas".equals(filtroNormalizado)
                ? sistema.stream().filter(n -> !n.leida()).toList()
                : sistema;

        model.addAttribute("notificaciones", visibles);
        model.addAttribute("sinLeer", sinLeer);
        model.addAttribute("filtro", filtroNormalizado);
        return "notificaciones";
    }

    @PostMapping("/todas-leidas")
    public String todasLeidas(Authentication authentication, HttpServletRequest request, HttpSession session) {
        notificacionesUsuarioService.marcarTodasLeidas(request, authentication, session);
        return "redirect:/notificaciones";
    }

    @PostMapping("/{id}/leida")
    public String leida(@PathVariable String id, HttpServletRequest request, HttpSession session,
                        @RequestParam(value = "filtro", defaultValue = "todas") String filtro) {
        notificacionesUsuarioService.marcarLeida(request, session, id);
        String filtroNormalizado = "noLeidas".equalsIgnoreCase(filtro) ? "noLeidas" : "todas";
        return "redirect:/notificaciones" + ("noLeidas".equals(filtroNormalizado) ? "?filtro=noLeidas" : "");
    }
}
