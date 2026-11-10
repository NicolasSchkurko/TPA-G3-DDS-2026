package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.services.EntidadContext;
import ar.edu.utn.frba.ddsi.front.services.NotificacionesDerivadasService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Atributos globales de la cabecera: rol activo, roles disponibles, cantidad de avisos sin leer.
 * Solo aplica a controladores MVC (las respuestas JSON los ignoran).
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalViewAdvice {

    /** Orden de presentación y de arranque (los roles de auto-registro primero; ADMIN al final). */
    private static final List<String> ORDEN_ROLES = List.of("ENTIDAD", "DONANTE", "CHOFER", "ADMIN");

    private final EntidadContext entidadContext;

    public GlobalViewAdvice(EntidadContext entidadContext) {
        this.entidadContext = entidadContext;
    }

    @ModelAttribute("rolesDisponibles")
    public List<String> rolesDisponibles(Authentication authentication) {
        return rolesDe(authentication);
    }

    @ModelAttribute("rolActivo")
    public String rolActivo(HttpSession session, Authentication authentication) {
        Object guardado = session.getAttribute("rolActivo");
        if (guardado instanceof String rol && rolesDe(authentication).contains(rol)) {
            return rol;
        }
        return rolesDe(authentication).stream().findFirst().orElse("ENTIDAD");
    }

    @ModelAttribute("usuarioNombre")
    public String usuarioNombre(Authentication authentication) {
        return authentication != null ? authentication.getName() : "";
    }

    /** Badge de notificaciones de la entidad (0 para otras vistas). */
    @ModelAttribute("notifNoLeidas")
    public long notifNoLeidas(HttpServletRequest request, HttpSession session, Authentication authentication) {
        String uri = request.getRequestURI() != null ? request.getRequestURI() : "";
        if (!uri.startsWith("/entidad") || !rolesDe(authentication).contains("ENTIDAD")) {
            return 0;
        }
        return NotificacionesDerivadasService.sinLeer(entidadContext.notificaciones(request, authentication, session));
    }

    private static List<String> rolesDe(Authentication authentication) {
        if (authentication == null) return List.of();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .collect(Collectors.toSet());
        return ORDEN_ROLES.stream().filter(roles::contains).toList();
    }
}
