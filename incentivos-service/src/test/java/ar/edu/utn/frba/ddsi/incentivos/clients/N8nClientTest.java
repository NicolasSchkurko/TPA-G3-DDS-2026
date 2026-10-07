package ar.edu.utn.frba.ddsi.incentivos.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPublicacionesPendientes;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@DisplayName("N8nClient: publicacion de insignias obtenidas")
class N8nClientTest {

    private static final String WEBHOOK = "http://localhost:5678/webhook/incentivos";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RepositorioPublicacionesPendientes pendientes;
    private N8nClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        pendientes = new RepositorioPublicacionesPendientes();
        client = new N8nClient(restTemplate, pendientes);
    }

    private void givenWebhook(String url) throws Exception {
        var field = N8nClient.class.getDeclaredField("n8nUrl");
        field.setAccessible(true);
        field.set(client, url);
    }

    private MisionCompletada eventoCompletada() {
        ImpactoDonacion impacto = new ImpactoDonacion(
                UUID.randomUUID(), UUID.randomUUID(),
                "Fundacion de prueba", 3,
                LocalDateTime.of(2026, 3, 1, 10, 0),
                "ALIMENTOS", "MERCEARIA", "ENTREGADA");

        return new MisionCompletada(
                "Primera donacion",
                "Primer paso",
                impacto.getIdUsuario(),
                "Ana",
                impacto
        );
    }

    @Test
    @DisplayName("publica en el webhook al completar una mision")
    void publicaAlCompletarLaMision() throws Exception {
        givenWebhook(WEBHOOK);

        server.expect(requestTo(WEBHOOK))
              .andExpect(jsonPath("$.redSocial").value("discord"))
              .andExpect(jsonPath("$.nomUsuario").value("Ana"))
              .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.publicarInsignia(eventoCompletada());

        server.verify();
        assertThat(pendientes.listarTodas()).isEmpty();
    }

    @Test
    @DisplayName("usa la insignia ganada en el prompt de la imagen")
    void usaLaInsigniaGanadaEnElPrompt() throws Exception {
        givenWebhook(WEBHOOK);

        server.expect(requestTo(WEBHOOK))
              .andExpect(jsonPath("$.prompt")
                          .value(org.hamcrest.Matchers.containsString("Primer paso")))
              .andExpect(jsonPath("$.mensaje")
                          .value(org.hamcrest.Matchers.containsString("Primera donacion")))
              .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.publicarInsignia(eventoCompletada());

        server.verify();
    }

    @Test
    @DisplayName("si el webhook falla, guarda la publicación como pendiente y NO propaga")
    void guardaLaPublicacionPendienteSiFalla() throws Exception {
        givenWebhook(WEBHOOK);

        server.expect(requestTo(WEBHOOK)).andRespond(withServerError());

        // El afterCommit no tiene try/catch: si la excepción sale, el donante recibe un 500
        // aunque la donación ya esté guardada, y el reintento suma progreso de nuevo.
        assertThatNoException().isThrownBy(() -> client.publicarInsignia(eventoCompletada()));

        assertThat(pendientes.listarTodas()).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("los pendientes no se duplican")
    void losPendientesNoSeDuplican() {
        PerfilPublicacionDTO publicacion =
                new PerfilPublicacionDTO("img", "texto", "discord", "Ana", UUID.randomUUID());

        pendientes.guardar(publicacion);
        pendientes.guardar(publicacion);

        assertThat(pendientes.listarTodas()).hasSize(1);

        pendientes.eliminar(publicacion);
        assertThat(pendientes.listarTodas()).isEmpty();
    }
}
