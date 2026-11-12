package ar.edu.utn.frba.ddsi.front.clients;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakRegistrationClientTest {

    private static final URI USUARIO_LOCATION =
            URI.create("http://keycloak/admin/realms/realm/users/07fe57d2-7b6a-4a61-a86f-cf2f69a6e8d1");

    private void esperarAlta(MockRestServiceServer server, boolean emailVerificadoEsperado) {
        server.expect(requestTo("http://keycloak/admin/realms/realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(body, Map.class);
                    assertEquals(emailVerificadoEsperado, json.get("emailVerified"));
                })
                .andRespond(withCreatedEntity(USUARIO_LOCATION));
    }

    @Test
    void conVerificacionDesactivadaCreaLaCuentaConEmailVerificado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeycloakRegistrationClient client = new KeycloakRegistrationClient(builder,
                "http://keycloak/token", "http://keycloak/admin", "realm", "front-registration", "secreto", false);

        server.expect(requestTo("http://keycloak/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON));
        esperarAlta(server, true);
        // asignarRoles pide un token nuevo antes de buscar el rol.
        server.expect(requestTo("http://keycloak/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak/admin/realms/realm/roles/DONANTE"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"role-id\",\"name\":\"DONANTE\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak/admin/realms/realm/users/07fe57d2-7b6a-4a61-a86f-cf2f69a6e8d1/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.crearUsuario("ana-donante", "password-segura", "ana@example.com", "DONANTE", "Ana", "Pérez");

        server.verify();
    }

    @Test
    void conVerificacionActivadaCreaLaCuentaSinEmailVerificado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeycloakRegistrationClient client = new KeycloakRegistrationClient(builder,
                "http://keycloak/token", "http://keycloak/admin", "realm", "front-registration", "secreto", true);

        server.expect(requestTo("http://keycloak/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON));
        esperarAlta(server, false);
        // asignarRoles pide un token nuevo antes de buscar el rol.
        server.expect(requestTo("http://keycloak/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak/admin/realms/realm/roles/DONANTE"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"role-id\",\"name\":\"DONANTE\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak/admin/realms/realm/users/07fe57d2-7b6a-4a61-a86f-cf2f69a6e8d1/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.crearUsuario("ana-donante", "password-segura", "ana@example.com", "DONANTE", "Ana", "Pérez");

        server.verify();
    }

    @Test
    void marcarEmailVerificadoActualizaLaCuenta() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeycloakRegistrationClient client = new KeycloakRegistrationClient(builder,
                "http://keycloak/token", "http://keycloak/admin", "realm", "front-registration", "secreto", true);

        server.expect(requestTo("http://keycloak/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak/admin/realms/realm/users/07fe57d2-7b6a-4a61-a86f-cf2f69a6e8d1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"emailVerified\":true}"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.marcarEmailVerificado("07fe57d2-7b6a-4a61-a86f-cf2f69a6e8d1");

        server.verify();
    }
}
