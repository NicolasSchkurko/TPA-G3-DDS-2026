package ar.edu.utn.frba.ddsi.incentivos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Reglas de acceso del servicio: todo abierto, igual que los otros módulos. La autorización de
 * admin se valida contra {@code donaciones-service} con el header {@code Admin-Id}.
 *
 * <p>Se conserva la clase con el filtro explícitamente abierto como lugar para reintroducir
 * una política si algún día se define.
 */
@Configuration
public class SecurityConfig {

    /** Deja pasar todo. CSRF desactivado: no hay estado de sesión ni cookies. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}