package ar.edu.utn.frba.ddsi.front.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ProfileRedirectHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String destination = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                ? "/panel"
                : authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DONANTE"))
                    ? "/donante"
                    : authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CHOFER"))
                        ? "/chofer"
                        : authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ENTIDAD"))
                            ? "/entidad" : "/sin-acceso";
        response.sendRedirect(request.getContextPath() + destination);
    }
}
