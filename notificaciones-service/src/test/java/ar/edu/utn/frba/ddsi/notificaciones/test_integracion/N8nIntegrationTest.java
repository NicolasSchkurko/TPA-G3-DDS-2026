package ar.edu.utn.frba.ddsi.notificaciones.test_integracion;

import ar.edu.utn.frba.ddsi.notificaciones.clientes.N8nClient;
import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionPayload;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** La llamada HTTP a n8n, contra un servidor simulado: sin n8n, MySQL ni Rabbit. */
class N8nIntegrationTest {

    private static final String URL_N8N = "http://n8n.test/webhook/notificaciones";

    private MockRestServiceServer servidor;
    private N8nClient client;

    @BeforeEach
    void preparar() {
        RestTemplate restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(restTemplate).build();

        client = new N8nClient(restTemplate);
        ReflectionTestUtils.setField(client, "n8nUrl", URL_N8N);
    }

    private NotificacionPayload payloadDePrueba() {
        return new NotificacionPayload(
                "email",
                "ana@test.com",
                new Mensaje("Asunto de prueba", "Cuerpo de prueba"));
    }

    @Test
    void deberiaPublicarElPayloadEnElWebhookConfigurado() {
        servidor.expect(requestTo(URL_N8N))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.canal", is("email")))
                .andExpect(jsonPath("$.direccionContacto", is("ana@test.com")))
                .andExpect(jsonPath("$.mensaje.asunto", is("Asunto de prueba")))
                .andExpect(jsonPath("$.mensaje.cuerpo", is("Cuerpo de prueba")))
                .andRespond(withSuccess());

        client.enviarNotificacion(payloadDePrueba());

        servidor.verify();
    }

    @Test
    void deberiaPropagarElErrorCuandoN8nNoEncuentraElWebhook() {
        // El 404 tiene que subir: es lo que deja la notificación FALLIDA.
        servidor.expect(requestTo(URL_N8N))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(HttpClientErrorException.NotFound.class,
                () -> client.enviarNotificacion(payloadDePrueba()));

        servidor.verify();
    }
}
