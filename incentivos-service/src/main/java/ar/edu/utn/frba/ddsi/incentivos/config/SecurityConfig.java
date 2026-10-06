package ar.edu.utn.frba.ddsi.incentivos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Reglas de acceso del servicio.
 *
 * <p>La autenticación es HTTP Basic y la superficie pública es un único endpoint: el perfil
 * público por id de usuario, que expone el nombre de usuario y su categoría actual y nada
 * más (punto 8). Todo lo demás pide credenciales.
 *
 * <p>Lo que esta clase NO hace es autorizar por rol: las operaciones de admin se validan
 * contra {@code donaciones-service} con el id que mandan en el header
 * {@code Admin-Id}. Está anotado en {@code PENDIENTES.md} (punto 1).
 */
@Configuration
public class SecurityConfig {

    /**
     * Deja libre solo el perfil público y exige credenciales en el resto.
     *
     * <p>La regla es por método y ruta exactos. Un {@code /api/perfiles/**} habría abierto
     * también el {@code GET /{idUsuario}}, que devuelve el perfil completo.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Única superficie sin autenticación (punto 8). El enunciado pide que
                        // la categoría actual sea visible públicamente, y este endpoint
                        // devuelve solo el nombre de usuario y el de su categoría: nada más.
                        // La regla es por método y ruta, no por prefijo: si en vez de esto
                        // fuera "/api/perfiles/**" quedaría abierto también el
                        // GET /{idUsuario} de más arriba, que devuelve el PerfilDTO
                        // completo. Todo lo demás sigue requiriendo credenciales.
                        .requestMatchers(HttpMethod.GET, "/api/perfiles/*/publico").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
