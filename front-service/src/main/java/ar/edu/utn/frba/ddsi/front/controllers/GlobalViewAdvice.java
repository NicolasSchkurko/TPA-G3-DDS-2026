package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.services.EntidadContext;
import ar.edu.utn.frba.ddsi.front.services.NotificacionesDerivadasService;
import ar.edu.utn.frba.ddsi.front.services.NotificacionesUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Atributos globales de la cabecera: rol activo, roles disponibles, cantidad de avisos sin leer
 * y la URL de la bandeja de notificaciones correspondiente.
 * Solo aplica a controladores MVC (las respuestas JSON los ignoran).
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalViewAdvice {

    /** Orden de presentación y de arranque (los roles de auto-registro primero; ADMIN al final). */
    private static final List<String> ORDEN_ROLES = List.of("ENTIDAD", "DONANTE", "CHOFER", "ADMIN");

    private final EntidadContext entidadContext;
    private final NotificacionesUsuarioService notificacionesUsuarioService;

    public GlobalViewAdvice(EntidadContext entidadContext,
                            NotificacionesUsuarioService notificacionesUsuarioService) {
        this.entidadContext = entidadContext;
        this.notificacionesUsuarioService = notificacionesUsuarioService;
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

    /** URL de la bandeja de avisos: la enriquecida de entidad o la genérica del sistema. */
    @ModelAttribute("notifUrl")
    public String notifUrl(HttpSession session, Authentication authentication, HttpServletRequest request) {
        return usarBandejaEntidad(session, authentication, request) ? "/entidad/notificaciones" : "/notificaciones";
    }

    /** Avisos sin leer del usuario autenticado (0 para anónimos). */
    @ModelAttribute("notifNoLeidas")
    public long notifNoLeidas(HttpServletRequest request, HttpSession session, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || rolesDe(authentication).isEmpty()) {
            return 0;
        }
        if (usarBandejaEntidad(session, authentication, request)) {
            return NotificacionesDerivadasService.sinLeer(entidadContext.notificaciones(request, authentication, session));
        }
        return notificacionesUsuarioService.sinLeer(request, authentication, session);
    }

    private static boolean usarBandejaEntidad(HttpSession session, Authentication authentication, HttpServletRequest request) {
        if (!rolesDe(authentication).contains("ENTIDAD")) return false;
        String uri = request.getRequestURI() != null ? request.getRequestURI() : "";
        String rol = rolActivoStatico(session, authentication);
        return uri.startsWith("/entidad") || "ENTIDAD".equals(rol);
    }

    private static String rolActivoStatico(HttpSession session, Authentication authentication) {
        Object guardado = session != null ? session.getAttribute("rolActivo") : null;
        if (guardado instanceof String rol && rolesDe(authentication).contains(rol)) {
            return rol;
        }
        return rolesDe(authentication).stream().findFirst().orElse("ENTIDAD");
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
