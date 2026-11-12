package ar.edu.utn.frba.ddsi.front.clients;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DonacionesClientRegistrationTest {

    @Test
    void donorRegistrationDoesNotRequireAdditionalContactOrAddress() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DonacionesClient client = new DonacionesClient(builder.build(), "http://localhost");

        server.expect(requestTo("http://localhost/api/personas"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String body = ((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString();
                    java.util.Map<String, Object> json = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(body, java.util.Map.class);
                    org.junit.jupiter.api.Assertions.assertEquals("HUMANA", json.get("tipoPersona"));
                    org.junit.jupiter.api.Assertions.assertFalse(json.containsKey("direccion"));
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> medios =
                            (java.util.List<java.util.Map<String, Object>>) json.get("mediosDeContacto");
                    org.junit.jupiter.api.Assertions.assertEquals(1, medios.size());
                    org.junit.jupiter.api.Assertions.assertEquals("EMAIL", medios.get(0).get("tipo"));
                    org.junit.jupiter.api.Assertions.assertEquals("ada@example.com", medios.get(0).get("valor"));
                })
                .andRespond(withSuccess());

        client.registrarUsuario("DONANTE", Map.of(
                "tipoPersona", "HUMANA",
                "nombre", "Ada",
                "apellido", "Lovelace",
                "edad", "30",
                "numeroDeDocumento", "12345678",
                "genero", "OTRO",
                "tipoContacto", "",
                "valorContacto", "",
                "email", "Ada@Example.com"
        ));

        server.verify();
    }

    @Test
    void includesBeneficiaryEmailInEntityProfileRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DonacionesClient client = new DonacionesClient(builder.build(), "http://localhost");

        server.expect(requestTo("http://localhost/api/entidades"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "razonSocial": "Entidad Ejemplo",
                          "correoElectronico": "entidad@example.com"
                        }
                        """))
                .andRespond(withSuccess());

        client.registrarUsuario("ENTIDAD", Map.of(
                "razonSocial", "Entidad Ejemplo",
                "rubro", "Social",
                "cuit", "30-12345678-9",
                "tipoJuridico", "ONG",
                "calleUno", "Calle de prueba",
                "altura", "123",
                "ciudad", "CABA",
                "provincia", "Buenos Aires",
                "pais", "Argentina",
                "correoElectronico", "entidad@example.com"
        ));

        server.verify();
    }
}
