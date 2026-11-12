package ar.edu.utn.frba.ddsi.front.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ar.edu.utn.frba.ddsi.front.dto.incentivos.IncentivosAdminDTOs;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("IncentivosClient: donaciones destacadas del landing")
class IncentivosClientTest {

    private static final String BASE_URL = "http://incentivos";

    private MockRestServiceServer server;
    private IncentivosClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new IncentivosClient(builder.build(), BASE_URL);
    }

    @Test
    @DisplayName("lee la lista pública sin datos personales")
    void leeLaListaPublica() {
        server.expect(requestTo(BASE_URL + "/api/donaciones/destacadas"))
                .andRespond(withSuccess("""
                        [{"idDonacion":"4d2f7a8e-0f5a-4e9e-9a0a-3f5c1d2e3f40",
                          "fechaEntrega":"2026-05-01T10:00:00",
                          "cantidadBienes":12,
                          "categoria":"ALIMENTOS",
                          "subCategoria":"MERCEARIA",
                          "entidadBeneficiaria":"Comedor Los Piletones"}]
                        """, MediaType.APPLICATION_JSON));

        var destacadas = client.obtenerDonacionesDestacadas();

        assertThat(destacadas).hasSize(1);
        assertThat(destacadas.get(0).cantidadBienes()).isEqualTo(12);
        assertThat(destacadas.get(0).categoria()).isEqualTo("ALIMENTOS");
        assertThat(destacadas.get(0).entidadBeneficiaria())
                .isEqualTo("Comedor Los Piletones");
        assertThat(destacadas.get(0).fechaEntrega())
                .isEqualTo(LocalDateTime.of(2026, 5, 1, 10, 0));
    }

    @Test
    @DisplayName("si incentivos falla, devuelve lista vacía")
    void unaCaidaDevuelveListaVacia() {
        server.expect(requestTo(BASE_URL + "/api/donaciones/destacadas"))
                .andRespond(withServerError());

        assertThat(client.obtenerDonacionesDestacadas()).isEmpty();
    }

    @Test
    @DisplayName("actualiza una misión con el ID del administrador")
    void actualizaMision() {
        UUID idAdmin = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID idMision = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        var mision = new IncentivosAdminDTOs.Mision();
        mision.setNombreMision("Misión editada");

        server.expect(requestTo(BASE_URL + "/api/misiones/admin/" + idMision))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Admin-Id", idAdmin.toString()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"nombreMision\":\"Misión editada\"}"))
                .andRespond(withSuccess());

        client.actualizarMision(idAdmin, idMision, mision);
    }
}
