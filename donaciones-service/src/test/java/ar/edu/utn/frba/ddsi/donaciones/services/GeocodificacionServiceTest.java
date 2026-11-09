package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeocodificacionServiceTest {

    @Test
    void geocodificarConsultaNominatimYReutilizaLasCoordenadasEnCache() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(request -> assertEquals("/search", request.getURI().getPath()))
                .andExpect(queryParam("street", "Av. San Juan 1234"))
                .andExpect(queryParam("city", "CABA"))
                .andExpect(queryParam("state", "Buenos Aires"))
                .andExpect(queryParam("country", "Argentina"))
                .andExpect(header("User-Agent", "DDSI-test/1.0"))
                .andRespond(withSuccess("[{\"lat\":\"-34.6200\",\"lon\":\"-58.4300\"}]",
                        MediaType.APPLICATION_JSON));
        GeocodificacionService service = new GeocodificacionService(
                restTemplate, "https://nominatim.test", "DDSI-test/1.0");

        Direccion direccion = new Direccion(
                "Av. San Juan", null, 1234, 0, null,
                new Ciudad("CABA", new Provincia("Buenos Aires", new Pais("Argentina"))));

        Optional<GeocodificacionService.Coordenadas> primera = service.geocodificar(direccion);
        Optional<GeocodificacionService.Coordenadas> desdeCache = service.geocodificar(direccion);

        assertTrue(primera.isPresent());
        assertEquals(-34.6200, primera.get().latitud());
        assertEquals(-58.4300, primera.get().longitud());
        assertEquals(primera, desdeCache);
        server.verify();
    }
}
