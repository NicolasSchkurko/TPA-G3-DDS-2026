package ar.edu.utn.frba.ddsi.donaciones.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * El front autentica y autoriza admins. Este servicio solo acepta tráfico de la red interna.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthorizationManager<RequestAuthorizationContext> aclInterna) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().access(aclInterna));
        return http.build();
    }

    /** Permite solo IPs/CIDR extra y hostnames de servicios internos. */
    @Bean
    AuthorizationManager<RequestAuthorizationContext> aclInterna(
            @Value("${seguridad.ips-internas:127.0.0.1,0:0:0:0:0:0:0:1}")
            String ipsPermitidas,
            @Value("${seguridad.servicios-internos:incentivos-service,front-service}")
            String serviciosInternos) {
        List<IpAddressMatcher> redes = Arrays.stream(ipsPermitidas.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();
        List<String> servicios = Arrays.stream(serviciosInternos.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .toList();
        return (authentication, context) -> {
            String origen = context.getRequest().getRemoteAddr();
            boolean permitida = redes.stream().anyMatch(red -> red.matches(origen))
                    || esIpDeServicio(origen, servicios);
            return new AuthorizationDecision(permitida);
        };
    }

    private static boolean esIpDeServicio(String origen, List<String> servicios) {
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
