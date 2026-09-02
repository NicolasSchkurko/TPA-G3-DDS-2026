package ar.edu.utn.frba.ddsi.incentivos.config;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorPerfiles;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class PerfilAuthenticationFilter extends OncePerRequestFilter {
    private static final String USER_ID_HEADER = "X-User-Id";

    private final GestorPerfiles gestorPerfiles;

    public PerfilAuthenticationFilter(GestorPerfiles gestorPerfiles) {
        this.gestorPerfiles = gestorPerfiles;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (userIdHeader != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UUID userId = UUID.fromString(userIdHeader);
                Perfil perfil = gestorPerfiles.obtenerPerfil(userId);

                if (perfil != null && perfil.getRole() != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            perfil.getIdUsuario(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + perfil.getRole().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (IllegalArgumentException ignored) {
                // La petición continuará sin autenticación y será rechazada por SecurityConfig.
            }
        }

        filterChain.doFilter(request, response);
    }
}
