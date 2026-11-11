package ar.edu.utn.frba.ddsi.notificaciones.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Solo acepta pedidos HTTP de los servicios internos o de las IPs extra configuradas.
 * El canal normal entre servicios es RabbitMQ; este filtro cubre el endpoint HTTP de respaldo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalIpFilter extends OncePerRequestFilter {

    private final List<IpAddressMatcher> redes;
    private final List<String> servicios;

    public InternalIpFilter(
            @Value("${seguridad.ips-internas:127.0.0.1,0:0:0:0:0:0:0:1}")
            String ipsPermitidas,
            @Value("${seguridad.servicios-internos:incentivos-service,donaciones-service}")
            String serviciosInternos) {
        this.redes = Arrays.stream(ipsPermitidas.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();
        this.servicios = Arrays.stream(serviciosInternos.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/notificaciones")) {
            filterChain.doFilter(request, response);
            return;
        }
        String origen = request.getRemoteAddr();
        if (redes.stream().anyMatch(red -> red.matches(origen))
                || esIpDeServicio(origen)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "El acceso HTTP a notificaciones está restringido a servicios internos.");
    }

    private boolean esIpDeServicio(String origen) {
        for (String servicio : servicios) {
            try {
                for (InetAddress direccion : InetAddress.getAllByName(servicio)) {
                    if (direccion.getHostAddress().equals(origen)) {
                        return true;
                    }
                }
            } catch (UnknownHostException ignored) {
                // En local el nombre no resuelve; la allowlist fija sigue aplicando.
            }
        }
        return false;
    }
}
