package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.services.NotificacionesDerivadasService;
import ar.edu.utn.frba.ddsi.front.services.NotificacionesUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

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
                                 Authentication authentication, HttpServletRequest request, Model model) {
        String filtroNormalizado = "noLeidas".equalsIgnoreCase(filtro) ? "noLeidas" : "todas";
        List<NotificacionesDerivadasService.Notificacion> sistema =
                notificacionesUsuarioService.notificaciones(request, authentication);
        long sinLeer = sistema.stream().filter(n -> !n.leida()).count();
        List<NotificacionesDerivadasService.Notificacion> visibles = "noLeidas".equals(filtroNormalizado)
                ? sistema.stream().filter(n -> !n.leida()).toList()
                : sistema;

        model.addAttribute("notificaciones", visibles);
        model.addAttribute("sinLeer", sinLeer);
        model.addAttribute("filtro", filtroNormalizado);
        return "notificaciones";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id,
                          @RequestParam(value = "filtro", defaultValue = "todas") String filtro,
                          Authentication authentication, HttpServletRequest request, Model model) {
        NotificacionesDerivadasService.Notificacion notificacion =
                notificacionesUsuarioService.notificaciones(request, authentication).stream()
                        .filter(n -> n.id().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        notificacionesUsuarioService.marcarLeida(request, authentication, id);
        model.addAttribute("notificacion", new NotificacionesDerivadasService.Notificacion(
                notificacion.id(), notificacion.tipo(), notificacion.titulo(), notificacion.detalle(),
                notificacion.fecha(), notificacion.idDonacion(), true, notificacion.origen(), notificacion.meta()));
        model.addAttribute("filtro", "noLeidas".equalsIgnoreCase(filtro) ? "noLeidas" : "todas");
        return "detalle-notificacion";
    }

    @PostMapping("/todas-leidas")
    public String todasLeidas(Authentication authentication, HttpServletRequest request) {
        notificacionesUsuarioService.marcarTodasLeidas(request, authentication);
        return "redirect:/notificaciones";
    }

    @PostMapping("/{id}/leida")
    public String leida(@PathVariable String id, Authentication authentication, HttpServletRequest request,
                        @RequestParam(value = "filtro", defaultValue = "todas") String filtro) {
        notificacionesUsuarioService.marcarLeida(request, authentication, id);
        String filtroNormalizado = "noLeidas".equalsIgnoreCase(filtro) ? "noLeidas" : "todas";
        return "redirect:/notificaciones" + ("noLeidas".equals(filtroNormalizado) ? "?filtro=noLeidas" : "");
    }
}
