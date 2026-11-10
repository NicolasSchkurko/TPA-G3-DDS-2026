package ar.edu.utn.frba.ddsi.front.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ProfileRedirectHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
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
                .filter(List.of("ENTIDAD", "DONANTE", "CHOFER", "ADMIN")::contains)
                .sorted(java.util.Comparator.comparingInt(List.of("ENTIDAD", "DONANTE", "CHOFER", "ADMIN")::indexOf))
                .collect(java.util.stream.Collectors.toList());
    }

    private static String destino(String rol) {
        return switch (rol) {
            case "ADMIN" -> "/panel";
            case "DONANTE" -> "/donante";
            case "CHOFER" -> "/chofer";
            default -> "/entidad";
        };
    }
}
