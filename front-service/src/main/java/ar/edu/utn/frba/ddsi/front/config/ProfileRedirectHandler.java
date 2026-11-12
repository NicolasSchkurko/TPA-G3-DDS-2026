package ar.edu.utn.frba.ddsi.front.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Component
public class ProfileRedirectHandler implements AuthenticationSuccessHandler {
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Si el usuario intentó donar antes de loguearse, retomamos el flujo de donación.
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null && esRutaDeDonacionGuardada(savedRequest, request.getContextPath())) {
            requestCache.removeRequest(request, response);
            response.sendRedirect(request.getContextPath() + "/realizar-donacion");
            return;
        }

        Object rolActivo = request.getSession().getAttribute("rolActivo");
        if (rolActivo instanceof String rol && roles(authentication).contains(rol)) {
            response.sendRedirect(request.getContextPath() + destino(rol));
            return;
        }
        String destination = roles(authentication).stream().findFirst()
                .map(ProfileRedirectHandler::destino)
                .orElse("/sin-acceso");
        response.sendRedirect(request.getContextPath() + destination);
    }

    private static List<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .filter(List.of("CHOFER", "ENTIDAD", "DONANTE", "ADMIN")::contains)
                .sorted(java.util.Comparator.comparingInt(List.of("CHOFER", "ENTIDAD", "DONANTE", "ADMIN")::indexOf))
                .collect(java.util.stream.Collectors.toList());
    }

    private static String destino(String rol) {
        return switch (rol) {
            case "ADMIN" -> "/admin";
            case "DONANTE" -> "/donante/mapa";
            case "CHOFER" -> "/chofer";
            default -> "/entidad";
        };
    }

    private static boolean esRutaDeDonacionGuardada(SavedRequest savedRequest, String contextPath) {
        try {
            String path = URI.create(savedRequest.getRedirectUrl()).getPath();
            return (contextPath + "/realizar-donacion").equals(path);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
