package ar.edu.utn.frba.ddsi.front.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;

/** Provisiona usuarios de la web en Keycloak desde el backend; nunca expone credenciales administrativas al navegador. */
@Component
public class KeycloakRegistrationClient {
    private static final Logger log = LoggerFactory.getLogger(KeycloakRegistrationClient.class);

    private final RestClient restClient;
    private final String tokenUri;
    private final String adminBaseUri;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakRegistrationClient(RestClient.Builder builder,
                                      @Value("${keycloak.token-uri}") String tokenUri,
                                      @Value("${keycloak.admin-base-uri}") String adminBaseUri,
                                      @Value("${keycloak.realm}") String realm,
                                      @Value("${keycloak.registration-client-id}") String clientId,
                                      @Value("${keycloak.registration-client-secret}") String clientSecret) {
        this.restClient = builder.build();
        this.tokenUri = tokenUri;
        this.adminBaseUri = adminBaseUri;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String crearUsuario(String username, String password, String email, String role) {
        return crearUsuario(username, password, email, role, null, null);
    }

    /**
     * Crea la cuenta en Keycloak. Si se pasan {@code firstName}/{@code lastName} se guardan como
     * nombre visible; si no se pasan, se usa el username para que el perfil declarativo de
     * Keycloak quede completo y no se dispare el requerido "Verify Profile".
     *
     * <p>Firma canónica (nosotros): {@code (username, password, email, role, firstName, lastName)}.
     * Sofi usaba otro orden de parámetros; se adaptó para no perder sus llamadas.</p>
     */
    public String crearUsuario(String username, String password, String email, String role,
                               String firstName, String lastName) {
        String token = obtenerToken();
        String nombre = firstName != null && !firstName.isBlank() ? firstName.trim() : username;
        String apellido = lastName != null && !lastName.isBlank() ? lastName.trim() : nombre;
        Map<String, Object> nuevoUsuario = new LinkedHashMap<>();
        nuevoUsuario.put("username", username);
        nuevoUsuario.put("enabled", true);
        if (email != null && !email.isBlank()) nuevoUsuario.put("email", email.trim().toLowerCase());
        nuevoUsuario.put("emailVerified", true);
        nuevoUsuario.put("firstName", nombre);
        nuevoUsuario.put("lastName", apellido);
        nuevoUsuario.put("credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));
        // Keycloak devuelve el identificador en el header Location al responder 201.
        URI location = restClient.post().uri(adminBaseUri + "/realms/{realm}/users", realm)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(nuevoUsuario)
                .retrieve().toBodilessEntity().getHeaders().getLocation();
        if (location == null) throw new IllegalStateException("Keycloak no devolvió el identificador de la cuenta creada.");
        String userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        try {
            asignarRoles(userId, List.of(role));
        } catch (RuntimeException e) {
            restClient.delete().uri(adminBaseUri + "/realms/{realm}/users/{userId}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve().toBodilessEntity();
            throw e;
        }
        return userId;
    }

    /** Asigna roles de realm al usuario. Los roles que no existen se omiten con un aviso. */
    public void asignarRoles(String userId, java.util.Collection<String> roles) {
        if (userId == null || roles == null || roles.isEmpty()) return;
        String token = obtenerToken();
        for (String role : roles) {
            if (role == null || role.isBlank()) continue;
            String normalizado = role.trim().toUpperCase();
            try {
                Map<?, ?> roleRepresentation = restClient.get()
                        .uri(adminBaseUri + "/realms/{realm}/roles/{role}", realm, normalizado)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve().body(Map.class);
                if (roleRepresentation == null) {
                    log.warn("El rol {} no existe en Keycloak; se omite.", normalizado);
                    continue;
                }
                restClient.post().uri(adminBaseUri + "/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(roleRepresentation)).retrieve().toBodilessEntity();
            } catch (RuntimeException e) {
                log.warn("No se pudo asignar el rol {} al usuario {}: {}", normalizado, userId, e.getMessage());
            }
        }
    }

    /** Roles de realm asignados a los usuarios de Keycloak que comparten el email. */
    public java.util.Set<String> rolesDeUsuariosConEmail(String email) {
        String token = obtenerToken();
        java.util.Set<String> roles = new java.util.LinkedHashSet<>();
        try {
            List<Map<String, Object>> usuarios = usuariosConEmail(email);
            for (Map<String, Object> usuario : usuarios) {
                Object id = usuario.get("id");
                if (id == null) continue;
                roles.addAll(rolesDeUsuario(id.toString()));
            }
        } catch (RuntimeException e) {
            log.warn("No se pudieron buscar los usuarios de Keycloak con el email {}: {}", email, e.getMessage());
        }
        return roles;
    }

    /** Roles de realm asignados a un usuario de Keycloak. */
    public java.util.Set<String> rolesDeUsuario(String userId) {
        if (userId == null || userId.isBlank()) return java.util.Set.of();
        String token = obtenerToken();
        java.util.Set<String> roles = new java.util.LinkedHashSet<>();
        try {
            List<Map<String, Object>> mappings = restClient.get()
                    .uri(adminBaseUri + "/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (mappings == null) return roles;
            for (Map<String, Object> mapping : mappings) {
                Object name = mapping.get("name");
                if (name != null) {
                    roles.add(name.toString().toUpperCase());
                }
            }
        } catch (RuntimeException e) {
            log.warn("No se pudieron obtener los roles de Keycloak del usuario {}: {}", userId, e.getMessage());
        }
        return roles;
    }

    public void eliminarUsuario(String userId) {
        String token = obtenerToken();
        restClient.delete().uri(adminBaseUri + "/realms/{realm}/users/{userId}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve().toBodilessEntity();
    }

    /** true si existe una cuenta de Keycloak con el email (insensible a mayúsculas). */
    public boolean existeUsuarioConEmail(String email) {
        return !usuariosConEmail(email).isEmpty();
    }

    /** true si existe una cuenta de Keycloak con el username (insensible a mayúsculas). */
    public boolean existeUsuarioConUsername(String username) {
        return !usuariosConUsername(username).isEmpty();
    }

    /** Id de la primera cuenta de Keycloak con el email indicado; null si no existe. */
    public String buscarUsuarioIdPorEmail(String email) {
        return usuariosConEmail(email).stream()
                .map(u -> u.get("id"))
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> usuariosConEmail(String email) {
        if (email == null || email.isBlank()) return List.of();
        String token = obtenerToken();
        try {
            List<Map<String, Object>> usuarios = restClient.get()
                    .uri(adminBaseUri + "/realms/{realm}/users?email={email}&exact=true", realm, email.trim().toLowerCase())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return usuarios != null ? usuarios : List.of();
        } catch (RuntimeException e) {
            log.warn("No se pudieron buscar los usuarios de Keycloak con el email {}: {}", email, e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> usuariosConUsername(String username) {
        if (username == null || username.isBlank()) return List.of();
        String token = obtenerToken();
        try {
            List<Map<String, Object>> usuarios = restClient.get()
                    .uri(adminBaseUri + "/realms/{realm}/users?username={username}&exact=true", realm, username.trim())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return usuarios != null ? usuarios : List.of();
        } catch (RuntimeException e) {
            log.warn("No se pudieron buscar los usuarios de Keycloak con el username {}: {}", username, e.getMessage());
            return List.of();
        }
    }

    private String obtenerToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<?, ?> response = restClient.post().uri(tokenUri).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(Map.class);
        if (response == null || response.get("access_token") == null)
            throw new IllegalStateException("Keycloak no devolvió un token para registrar la cuenta.");
        return response.get("access_token").toString();
    }
}
