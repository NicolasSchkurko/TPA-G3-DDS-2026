package ar.edu.utn.frba.ddsi.incentivos.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioNotificacionesPendientes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@DisplayName("NotificacionClient: envio de notificaciones")
class NotificacionClientTest {

    private static final String BASE_URL = "http://localhost:8083";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RepositorioNotificacionesPendientes pendientes;
    private NotificacionClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        pendientes = new RepositorioNotificacionesPendientes();
        client = new NotificacionClient(restTemplate, null, pendientes);
    }

    private void givenBaseUrl(String url) throws Exception {
        var field = NotificacionClient.class.getDeclaredField("notificacionesUrl");
        field.setAccessible(true);
        field.set(client, url);
    }

    private PerfilNotificacionDTO notificacion() {
        return new PerfilNotificacionDTO("EMAIL", "ana@example.com", "Mensaje", "Asunto");
    }

    @Test
    @DisplayName("envía la notificación al servicio de notificaciones")
    void enviaLaNotificacion() throws Exception {
        givenBaseUrl(BASE_URL);

        server.expect(requestTo(BASE_URL))
              .andRespond(withSuccess());

        client.enviarNotificacion(notificacion());

        server.verify();
        assertThat(pendientes.listarTodas()).isEmpty();
    }

    @Test
    @DisplayName("si el envío falla, guarda la notificación como pendiente y propaga el error")
    void guardaLaNotificacionPendienteSiFalla() throws Exception {
        givenBaseUrl(BASE_URL);

        server.expect(requestTo(BASE_URL))
              .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.enviarNotificacion(notificacion()))
                .isInstanceOf(EnvioNotificacionException.class);

        assertThat(pendientes.listarTodas()).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("envía el cuerpo con medio, dirección, mensaje y asunto")
    void enviaElCuerpoEsperado() throws Exception {
        givenBaseUrl(BASE_URL);

        server.expect(requestTo(BASE_URL))
              .andExpect(jsonPath("$.direccionContacto").value("ana@example.com"))
              .andExpect(jsonPath("$.medioDeContacto").value("EMAIL"))
              .andExpect(jsonPath("$.cuerpoMensaje").value("Mensaje"))
              .andExpect(jsonPath("$.asuntoMensaje").value("Asunto"))
              .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.enviarNotificacion(notificacion());

        server.verify();
    }
}
