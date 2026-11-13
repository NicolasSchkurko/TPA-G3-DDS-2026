package ar.edu.utn.frba.ddsi.front.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class NotificacionesServicioClientTest {

    private static final String BASE_URL = "http://notificaciones/api";
    private MockRestServiceServer server;
    private NotificacionesServicioClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NotificacionesServicioClient(builder.build(), BASE_URL);
    }

    @Test
    void consultaLasLecturasPersistidasDelUsuario() {
        server.expect(requestTo(BASE_URL + "/notificaciones/lecturas?usuarioId=usuario-123"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        ["e3544220-4bfc-4dcf-8938-d86dbdc047af"]
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.listarLeidas("usuario-123"))
                .containsExactly("e3544220-4bfc-4dcf-8938-d86dbdc047af");
        server.verify();
    }

    @Test
    void persisteIdsDeNotificacionesLeidas() {
        server.expect(requestTo(BASE_URL + "/notificaciones/lecturas"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {
                          "usuarioId": "usuario-123",
                          "notificacionIds": ["e3544220-4bfc-4dcf-8938-d86dbdc047af"]
                        }
                        """))
                .andRespond(withNoContent());

        client.marcarLeidas("usuario-123", Set.of("e3544220-4bfc-4dcf-8938-d86dbdc047af"));
        server.verify();
    }
}
