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

@Component
public class ProfileRedirectHandler implements AuthenticationSuccessHandler {
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null && esRutaDeDonacionGuardada(savedRequest, request.getContextPath())) {
            requestCache.removeRequest(request, response);
            response.sendRedirect(request.getContextPath() + "/realizar-donacion");
            return;
        }

        String destination = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                ? "/panel"
                : authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DONANTE"))
                    ? "/donante/mapa"
                    : authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CHOFER"))
                        ? "/chofer"
                        : authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ENTIDAD"))
                            ? "/entidad" : "/sin-acceso";
        response.sendRedirect(request.getContextPath() + destination);
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
