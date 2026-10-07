package ar.edu.utn.frba.ddsi.incentivos.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@DisplayName("DonacionClient: integracion con el servicio de donaciones")
class DonacionClientTest {

    private static final String BASE_URL = "http://localhost:8084";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private DonacionClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new DonacionClient(restTemplate);
    }

    /** La URL se inyecta por @Value: en tests se fija por reflexión. */
    private void givenBaseUrl(String url) throws Exception {
        var field = DonacionClient.class.getDeclaredField("donacionesUrl");
        field.setAccessible(true);
        field.set(client, url);
    }

    @Test
    @DisplayName("mapea el primer medio de contacto de la persona")
    void mapeaElMedioDeContacto() throws Exception {
        givenBaseUrl(BASE_URL);
        UUID idUsuario = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/personas/" + idUsuario + "/medios-contacto"))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess(
                      "[{\"tipo\":\"EMAIL\",\"valor\":\"ana@example.com\"}]",
                      MediaType.APPLICATION_JSON));

        MedioContacto contacto = client.obtenerContactoPersona(idUsuario);

        assertThat(contacto).isNotNull();
        assertThat(contacto.getMedioDeContacto()).isEqualTo("EMAIL");
        assertThat(contacto.getDireccionContacto()).isEqualTo("ana@example.com");
        server.verify();
    }

    @Test
    @DisplayName("devuelve null si la persona no tiene medios de contacto")
    void devuelveNullSiNoHayContactos() throws Exception {
        givenBaseUrl(BASE_URL);
        UUID idUsuario = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/personas/" + idUsuario + "/medios-contacto"))
              .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.obtenerContactoPersona(idUsuario)).isNull();
        server.verify();
    }

    @Test
    @DisplayName("devuelve null si el servicio de donaciones falla")
    void devuelveNullSiElServicioFalla() throws Exception {
        givenBaseUrl(BASE_URL);
        UUID idUsuario = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/personas/" + idUsuario + "/medios-contacto"))
              .andRespond(withServerError());

        assertThat(client.obtenerContactoPersona(idUsuario)).isNull();
        server.verify();
    }

    @Test
    @DisplayName("normaliza la URL base aunque termine con barras")
    void normalizaLaUrlBase() throws Exception {
        givenBaseUrl(BASE_URL + "/// ");
        UUID idUsuario = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/personas/" + idUsuario + "/medios-contacto"))
              .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.obtenerContactoPersona(idUsuario);

        server.verify();
    }

    @Test
    @DisplayName("verificarAdmin da true solo si el admin existe")
    void verificaAdminExistente() throws Exception {
        givenBaseUrl(BASE_URL);
        UUID idAdmin = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/admins/" + idAdmin))
              .andRespond(withStatus(HttpStatus.OK));

        assertThat(client.verificarAdmin(idAdmin)).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("verificarAdmin da false con 404")
    void verificaAdminInexistente() throws Exception {
        givenBaseUrl(BASE_URL);
        UUID idAdmin = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/admins/" + idAdmin))
              .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.verificarAdmin(idAdmin)).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("verificarAdmin da false con error de servidor")
    void verificaAdminConErrorDeServidor() throws Exception {
        givenBaseUrl(BASE_URL);
        UUID idAdmin = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/admins/" + idAdmin))
              .andRespond(withServerError());

        assertThat(client.verificarAdmin(idAdmin)).isFalse();
        server.verify();
    }
}
