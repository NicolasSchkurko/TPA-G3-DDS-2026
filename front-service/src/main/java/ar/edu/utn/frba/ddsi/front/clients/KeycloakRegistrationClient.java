package ar.edu.utn.frba.ddsi.front.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

/** Provisiona usuarios de la web en Keycloak desde el backend; nunca expone credenciales administrativas al navegador. */
@Component
public class KeycloakRegistrationClient {
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

    public String crearUsuario(String username, String password, String role,
                               String firstName, String lastName, String email) {
        String token = obtenerToken();
        Map<String, Object> user = new java.util.LinkedHashMap<>();
        user.put("username", username);
        user.put("enabled", true);
        user.put("credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));
        if (firstName != null && !firstName.isBlank()) user.put("firstName", firstName);
        if (lastName != null && !lastName.isBlank()) user.put("lastName", lastName);
        if (email != null && !email.isBlank()) user.put("email", email);
        // Keycloak devuelve el identificador en el header Location al responder 201.
        URI location = restClient.post().uri(adminBaseUri + "/realms/{realm}/users", realm)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve().toBodilessEntity().getHeaders().getLocation();
        if (location == null) throw new IllegalStateException("Keycloak no devolvió el identificador de la cuenta creada.");
        String userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        try {
            Map<?, ?> roleRepresentation = restClient.get()
                    .uri(adminBaseUri + "/realms/{realm}/roles/{role}", realm, role)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve().body(Map.class);
            if (roleRepresentation == null) throw new IllegalStateException("El rol solicitado no existe en Keycloak.");
            restClient.post().uri(adminBaseUri + "/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(roleRepresentation)).retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            restClient.delete().uri(adminBaseUri + "/realms/{realm}/users/{userId}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve().toBodilessEntity();
            throw e;
        }
        return userId;
    }

    public void eliminarUsuario(String userId) {
        String token = obtenerToken();
        restClient.delete().uri(adminBaseUri + "/realms/{realm}/users/{userId}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve().toBodilessEntity();
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
