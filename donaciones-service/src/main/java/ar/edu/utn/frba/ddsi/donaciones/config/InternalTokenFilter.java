package ar.edu.utn.frba.ddsi.donaciones.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege los endpoints internos de identidad con un token compartido.
 *
 * <p>{@code donaciones-service} no tiene autenticación propia: sólo debe ser accesible
 * desde {@code front-service} (u otro caller autorizado). Los servicios internos que
 * necesitan hablar con este servicio envían el header {@code X-Internal-Token}.</p>
 *
 * <p>Si {@code INTERNAL_TOKEN} está vacío (modo desarrollo/test), el filtro no exige nada.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";

    private final String internalToken;

    public InternalTokenFilter(@Value("${INTERNAL_TOKEN:}") String internalToken) {
        this.internalToken = internalToken == null ? "" : internalToken.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (internalToken.isEmpty()) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.startsWith("/api/identidades");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibido = request.getHeader(HEADER);
        if (recibido == null || !seguro(recibido, internalToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Se requiere el token interno.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Compara en tiempo constante para no filtrar el token por timing. */
    private static boolean seguro(String recibido, String esperado) {
        byte[] a = recibido.getBytes(StandardCharsets.UTF_8);
        byte[] b = esperado.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
