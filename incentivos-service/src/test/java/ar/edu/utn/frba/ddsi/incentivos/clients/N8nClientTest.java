package ar.edu.utn.frba.ddsi.incentivos.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.services.PublicacionesN8nService;
import ar.edu.utn.frba.ddsi.incentivos.services.PublicacionesN8nService.PublicacionReclamada;
import java.time.LocalDateTime;
import java.util.Optional;
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
    private PublicacionesN8nService publicaciones;
    private N8nClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        publicaciones = org.mockito.Mockito.mock(PublicacionesN8nService.class);
        client = new N8nClient(restTemplate, publicaciones);
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
    @DisplayName("encola la publicacion al completar una mision")
    void encolaLaPublicacionAlCompletarLaMision() {
        client.encolarInsignia(eventoCompletada());

        var publicacion = org.mockito.ArgumentCaptor.forClass(PerfilPublicacionDTO.class);
        verify(publicaciones).encolar(publicacion.capture());
        assertThat(publicacion.getValue().getRedSocial()).isEqualTo("discord");
        assertThat(publicacion.getValue().getNomUsuario()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("publica una fila reclamada en el webhook y la elimina al confirmarse")
    void publicaUnaFilaReclamada() throws Exception {
        givenWebhook(WEBHOOK);
        UUID id = UUID.randomUUID();
        PerfilPublicacionDTO contenido = new PerfilPublicacionDTO(
                "Primer paso", "Primera donacion", "discord", "Ana", UUID.randomUUID());
        when(publicaciones.reclamarSiguiente(any()))
                .thenReturn(Optional.of(new PublicacionReclamada(id, contenido, 1)))
                .thenReturn(Optional.empty());

        server.expect(requestTo(WEBHOOK))
              .andExpect(jsonPath("$.redSocial").value("discord"))
              .andExpect(jsonPath("$.nomUsuario").value("Ana"))
              .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.procesarPendientes();

        server.verify();
        verify(publicaciones).confirmar(id);
    }

    @Test
    @DisplayName("usa la insignia ganada en el prompt de la imagen")
    void usaLaInsigniaGanadaEnElPrompt() throws Exception {
        givenWebhook(WEBHOOK);
        PerfilPublicacionDTO contenido = new PerfilPublicacionDTO(
                "Primer paso", "Primera donacion", "discord", "Ana", UUID.randomUUID());
        when(publicaciones.reclamarSiguiente(any()))
                .thenReturn(Optional.of(new PublicacionReclamada(UUID.randomUUID(), contenido, 1)))
                .thenReturn(Optional.empty());

        server.expect(requestTo(WEBHOOK))
              .andExpect(jsonPath("$.prompt")
                          .value(org.hamcrest.Matchers.containsString("Primer paso")))
              .andExpect(jsonPath("$.mensaje")
                          .value(org.hamcrest.Matchers.containsString("Primera donacion")))
              .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.procesarPendientes();

        server.verify();
    }

    @Test
    @DisplayName("si el webhook falla, agenda el reintento y no lo propaga")
    void reintentaLaPublicacionSiFalla() throws Exception {
        givenWebhook(WEBHOOK);
        UUID id = UUID.randomUUID();
        PerfilPublicacionDTO contenido = new PerfilPublicacionDTO(
                "img", "texto", "discord", "Ana", UUID.randomUUID());
        when(publicaciones.reclamarSiguiente(any()))
                .thenReturn(Optional.of(new PublicacionReclamada(id, contenido, 1)))
                .thenReturn(Optional.empty());

        server.expect(requestTo(WEBHOOK)).andRespond(withServerError());

        client.procesarPendientes();

        verify(publicaciones).reintentar(org.mockito.ArgumentMatchers.eq(id), any(),
                org.mockito.ArgumentMatchers.anyString());
        server.verify();
    }
}
