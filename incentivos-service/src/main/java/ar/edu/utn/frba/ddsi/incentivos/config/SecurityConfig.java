package ar.edu.utn.frba.ddsi.incentivos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Única superficie sin autenticación (punto 8). El enunciado pide que
                        // la categoría actual sea visible públicamente, y este endpoint
                        // devuelve solo el nombre de usuario y el de su categoría: nada más.
                        //
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