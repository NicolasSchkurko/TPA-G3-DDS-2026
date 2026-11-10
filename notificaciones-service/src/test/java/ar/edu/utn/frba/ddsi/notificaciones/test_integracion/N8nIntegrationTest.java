package ar.edu.utn.frba.ddsi.notificaciones.test_integracion;

import ar.edu.utn.frba.ddsi.notificaciones.clients.N8nClient;
import ar.edu.utn.frba.ddsi.notificaciones.dto.n8n.NotificacionPayload;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void publicaElUrlMapaCuandoElMensajeLoTiene() {
        servidor.expect(requestTo(URL_N8N))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.urlMapa", is("https://mapa/1")))
                .andRespond(withSuccess());

        client.enviarNotificacion(new NotificacionPayload(
                "email", "ana@test.com",
                new Mensaje("Asunto de prueba", "Cuerpo de prueba", "https://mapa/1")));

        servidor.verify();
    }

    @Test
    void elInfoNoLogueaElCuerpoCompletoYElDebugSi() {
        Logger logger = (Logger) LoggerFactory.getLogger(N8nClient.class);
        Level nivelOriginal = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try {
            servidor.expect(requestTo(URL_N8N)).andRespond(withSuccess());
            client.enviarNotificacion(payloadDePrueba());
            servidor.verify();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(nivelOriginal);
        }

        String cuerpo = "Cuerpo de prueba";
        String info = appender.list.stream()
                .filter(evento -> evento.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        String debug = appender.list.stream()
                .filter(evento -> evento.getLevel() == Level.DEBUG)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        assertFalse(info.contains(cuerpo), "el INFO no debe llevar el cuerpo completo: " + info);
        assertTrue(info.contains("cuerpo=" + cuerpo.length() + " chars"), info);
        assertTrue(debug.contains(cuerpo), "el DEBUG sí puede llevar el cuerpo: " + debug);
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
