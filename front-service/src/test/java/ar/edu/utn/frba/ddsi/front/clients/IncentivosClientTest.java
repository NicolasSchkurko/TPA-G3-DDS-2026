package ar.edu.utn.frba.ddsi.front.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ar.edu.utn.frba.ddsi.front.dto.incentivos.IncentivosAdminDTOs;
import java.time.LocalDateTime;
import java.util.List;
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
    private static final UUID ADMIN_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

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
    @DisplayName("crea categorías con su posición, misiones y administrador")
    void creaCategoriaConDatosDeAdministracion() {
        IncentivosAdminDTOs.Categoria categoria = new IncentivosAdminDTOs.Categoria();
        categoria.setNombre("Frecuentes");
        categoria.setPosicionSecuencia(2);
        categoria.setMisiones(List.of(UUID.fromString("4d2f7a8e-0f5a-4e9e-9a0a-3f5c1d2e3f40")));

        server.expect(requestTo(BASE_URL + "/api/categorias/admin"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Admin-Id", ADMIN_ID.toString()))
                .andExpect(content().json("""
                        {"nombre":"Frecuentes","posicionSecuencia":2,
                         "misiones":["4d2f7a8e-0f5a-4e9e-9a0a-3f5c1d2e3f40"]}
                        """))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.crearCategoria(categoria, ADMIN_ID);
        server.verify();
    }

    @Test
    @DisplayName("actualiza categorías en el endpoint administrativo")
    void actualizaCategoria() {
        IncentivosAdminDTOs.Categoria categoria = new IncentivosAdminDTOs.Categoria();
        categoria.setNombre("Frecuentes");
        categoria.setPosicionSecuencia(1);
        categoria.setMisiones(List.of());
        UUID idCategoria = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/categorias/admin/" + idCategoria))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Admin-Id", ADMIN_ID.toString()))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.actualizarCategoria(idCategoria, categoria, ADMIN_ID);
        server.verify();
    }

    @Test
    @DisplayName("elimina categorías usando el endpoint administrativo")
    void eliminaCategoria() {
        UUID idCategoria = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/categorias/admin/" + idCategoria))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Admin-Id", ADMIN_ID.toString()))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withNoContent());

        client.eliminarCategoria(idCategoria, ADMIN_ID);
        server.verify();
    }
}
