package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.dto.SeguimientoDTO;
import ar.edu.utn.frba.ddsi.front.services.EntidadContext;
import ar.edu.utn.frba.ddsi.front.services.NotificacionesDerivadasService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Endpoints BFF para AJAX/polling del portal de entidad. Reutilizan el mismo caché por request
 * que las páginas, así el header y los widgets no duplican llamadas al backend.
 */
@RestController
@RequestMapping("/api/entidad")
public class EntidadApiController {

    private final EntidadContext entidadContext;

    public EntidadApiController(EntidadContext entidadContext) {
        this.entidadContext = entidadContext;
    }

    /** Estado completo de seguimiento para el polling del mapa. */
    @GetMapping("/seguimiento")
    public SeguimientoDTO seguimiento(Authentication authentication, HttpServletRequest request) {
        var entidad = entidadContext.entidad(request, authentication);
        if (entidad == null) {
            SeguimientoDTO vacio = new SeguimientoDTO();
            vacio.setGeneradoEn(OffsetDateTime.now().toString());
            return vacio;
        }
        return entidadContext.seguimiento(request, authentication);
    }

    /** Contador de avisos sin leer para el badge del header. */
    @GetMapping("/notificaciones/count")
    public Map<String, Object> count(Authentication authentication, HttpServletRequest request, HttpSession session) {
        List<NotificacionesDerivadasService.Notificacion> notificaciones =
                entidadContext.notificaciones(request, authentication, session);
        long sinLeer = NotificacionesDerivadasService.sinLeer(notificaciones);
        return Map.of("sinLeer", sinLeer);
    }

    /** Marca una notificación derivada como leída en la sesión del usuario. */
    @PostMapping("/notificaciones/{id}/leida")
    public ResponseEntity<Map<String, Object>> marcarLeida(@PathVariable String id, HttpSession session) {
        Set<String> leidas = entidadContext.leidasEnSesion(session);
        leidas.add(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}