package ar.edu.utn.frba.ddsi.front.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler profileRedirectHandler,
            GrantedAuthoritiesMapper keycloakAuthoritiesMapper,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.logout-uri}") String logoutUri,
            @Value("${keycloak.post-logout-redirect-uri}") String postLogoutRedirectUri
    ) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/", "/landing", "/login", "/register", "/sin-acceso",
                        "/seguimiento/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/realizar-donacion").authenticated()
                .requestMatchers("/notificaciones", "/notificaciones/**").authenticated()
                .requestMatchers("/donaciones/donantes").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/donaciones").permitAll()
                .requestMatchers(HttpMethod.GET, "/donaciones/{id}").permitAll()
                .requestMatchers("/panel", "/donaciones/**", "/donantes/**", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/reconciliacion/**").hasRole("ADMIN")
                .requestMatchers("/donante", "/donante/**").hasRole("DONANTE")
                .requestMatchers("/chofer/**").hasRole("CHOFER")
                .requestMatchers("/entidad/**", "/api/entidad/**").hasRole("ENTIDAD")
                .requestMatchers("/rol/**", "/cuenta/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/sin-acceso"))
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(keycloakAuthoritiesMapper))
                .successHandler(profileRedirectHandler)
            )
            .logout(logout -> logout.logoutSuccessHandler(new KeycloakLogoutSuccessHandler(
                    clientId, logoutUri, postLogoutRedirectUri)));

        return http.build();
    }

    @Bean
    GrantedAuthoritiesMapper keycloakAuthoritiesMapper(DonacionesClient donacionesClient) {
        return source -> {
            List<GrantedAuthority> authorities = new ArrayList<>(source);
            String email = null;
            for (GrantedAuthority authority : source) {
                if (!(authority instanceof OidcUserAuthority oidcAuthority)) continue;
                Map<String, Object> realmAccess = oidcAuthority.getIdToken().getClaimAsMap("realm_access");
                if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
                    for (Object role : roles) {
                        if (role instanceof String roleName) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
                        }
                    }
                }
                if (email == null) {
                    Object claim = oidcAuthority.getIdToken().getClaim("email");
                    if (claim instanceof String mail && !mail.isBlank()) {
                        email = mail.trim().toLowerCase();
                    }
                }
            }
            // El email es la identidad: si ya está vinculada a un donante/entidad/admin, habilitamos esos roles.
            if (email != null) {
                try {
                    IdentidadDTO identidad = donacionesClient.buscarIdentidadPorEmail(email);
                    if (identidad != null && identidad.getRoles() != null) {
                        for (String role : identidad.getRoles()) {
                            if (role == null || role.isBlank()) continue;
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        }
                    }
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                            .warn("No se pudo resolver la identidad por email {}: {}", email, e.getMessage());
                }
            }
            return authorities;
        };
    }
}
