package ar.edu.utn.frba.ddsi.front.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("DonacionesClient: estado del donante para reconciliar")
class DonacionesClientTest {

    private static final String BASE_URL = "http://donaciones";

    private MockRestServiceServer server;
    private DonacionesClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DonacionesClient(builder.build(), BASE_URL);
    }

    @Test
    @DisplayName("lee el activo del donante")
    void leeElActivo() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/personas/" + id))
                .andRespond(withSuccess("{\"activo\":true}", MediaType.APPLICATION_JSON));

        assertThat(client.donanteSigueActivo(id)).contains(true);
    }

    @Test
    @DisplayName("un activo null no se interpreta como baja")
    void activoNullNoEsBaja() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/personas/" + id))
                .andRespond(withSuccess("{\"activo\":null}", MediaType.APPLICATION_JSON));

        assertThat(client.donanteSigueActivo(id)).isEmpty();
    }

    @Test
    @DisplayName("404 se interpreta como donante inexistente")
    void notFoundEsDonanteInexistente() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/personas/" + id))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.donanteSigueActivo(id)).contains(false);
    }

    @Test
    @DisplayName("una caída no se interpreta como baja")
    void errorNoEsBaja() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/personas/" + id))
                .andRespond(withServerError());

        assertThat(client.donanteSigueActivo(id)).isEmpty();
    }

    @Test
    @DisplayName("el alta de admin manda el id de Keycloak")
    void altaDeAdminMandaElIdDeKeycloak() {
        UUID idKeycloak = UUID.randomUUID();
        Map<String, String> datos = Map.of(
                "nombreAMostrar", "Admin",
                "nombre", "Ana",
                "apellido", "Admin",
                "edad", "30",
                "numeroDeDocumento", "30123456",
                "genero", "MUJER",
                "tipoContacto", "WHATSAPP",
                "valorContacto", "1122334455");

        server.expect(requestTo(BASE_URL + "/api/admins"))
                .andExpect(jsonPath("$.id").value(idKeycloak.toString()))
                .andExpect(jsonPath("$.medioDeContacto.tipo").value("WHATSAPP"))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.registrarUsuario("ADMIN", datos, idKeycloak.toString());

        server.verify();
    }

}
