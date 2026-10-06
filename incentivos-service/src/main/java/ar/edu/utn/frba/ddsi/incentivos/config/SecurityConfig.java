package ar.edu.utn.frba.ddsi.incentivos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Reglas de acceso del servicio.
 *
 * <p><b>Sin autenticación, igual que los otros tres módulos.</b>
 *
 * <p>Antes exigía HTTP Basic en todo salvo el perfil público, y con la contraseña
 * autogenerada por Spring Boot. Eso lo convertía en un muro: los demás servicios no se
 * comunicaban con él. Y no había forma de resolverlo autenticando entre servicios, porque la
 * contraseña autogenerada cambia en cada arranque, así que no existe credencial fija que un
 * llamador pueda conocer. La consecuencia medida fue que {@code POST /api/perfiles} y
 * {@code PATCH /api/perfiles/donacion/{id}} devolvían 401 desde `donaciones-service`, y con eso la
 * cadena de donación a incentivo no funcionaba en ninguna parte.
 *
 * <p><b>Lo que se pierde es poco, y está anotado.</b> Se pierde la superficie pública del
 * perfil del punto 8, que queda abierto igual. Queda la autorización de las operaciones de
 * admin, que se validan contra `donaciones-service` con el header {@code Admin-Id}: eso es un
 * header que controla el cliente, así que nunca fue seguridad de verdad. Está anotado como
 * punto 1 del backlog.
 *
 * <p>Endurecer el acceso queda como trabajo de diseño con una política declarada, no como el
 * efecto secundario de haber puesto un muro que rompía la integración.
 *
 * <p><b>Se conserva la clase y no se borra Spring Security del pom.</b> Tener el filtro
 * declarado y explícitamente abierto deja la decisión a la vista y deja el lugar donde
 * reintroducir la política si algún día se define. Borrar la dependencia y la clase sería
 * menos explícito, no más seguro.
 */
@Configuration
public class SecurityConfig {

    /**
     * Deja pasar todo.
     *
     * <p>El CSRF va desactivado porque no hay estado de sesión: la autorización de admin se
     * valida por header contra `donaciones-service`, no por cookie, así que el CSRF no tiene
     * superficie que proteger en este servicio.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}