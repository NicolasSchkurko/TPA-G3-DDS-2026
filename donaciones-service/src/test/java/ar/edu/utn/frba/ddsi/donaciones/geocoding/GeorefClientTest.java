package ar.edu.utn.frba.ddsi.donaciones.geocoding;

import ar.edu.utn.frba.ddsi.donaciones.clients.GeorefClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.GeorefException;
import ar.edu.utn.frba.ddsi.donaciones.dto.georef.GeorefDTOs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GeorefClientTest {

    private static final String URL = "https://apis.datos.gob.ar/georef/api";

    private RestTemplate restTemplate;
    private GeorefClient cliente;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        cliente = new GeorefClient(restTemplate, URL + "/");
    }

    @Test
    @DisplayName("Manda el lote al POST /direcciones y devuelve los resultados")
    void consultaElEndpointDeDirecciones() {
        List<GeorefDTOs.Consulta> consultas = List.of(
                GeorefDTOs.Consulta.de("Av. Medrano 951", "Buenos Aires", "CABA"));
        List<GeorefDTOs.Resultado> resultados = List.of(
                new GeorefDTOs.Resultado(List.of()));
        when(restTemplate.postForObject(
                eq(URL + "/direcciones"), any(), eq(GeorefDTOs.Respuesta.class)))
                .thenReturn(new GeorefDTOs.Respuesta(resultados));

        assertEquals(resultados, cliente.buscar(consultas));

        ArgumentCaptor<GeorefDTOs.Lote> lote = ArgumentCaptor.forClass(GeorefDTOs.Lote.class);
        verify(restTemplate).postForObject(
                eq(URL + "/direcciones"), lote.capture(), eq(GeorefDTOs.Respuesta.class));
        assertEquals(consultas, lote.getValue().direcciones());
    }

    @Test
    @DisplayName("Un error HTTP se traduce en GeorefException")
    void errorHttpSeTraduceEnGeorefException() {
        when(restTemplate.postForObject(anyString(), any(), eq(GeorefDTOs.Respuesta.class)))
                .thenThrow(new RestClientException("timeout"));

        assertThrows(GeorefException.class, () -> cliente.buscar(
                List.of(GeorefDTOs.Consulta.de("Av. Medrano 951", null, null))));
    }

    @Test
    @DisplayName("Sin consultas no llama a la API")
    void sinConsultasNoLlama() {
        assertTrue(cliente.buscar(List.of()).isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Una respuesta sin resultados no rompe el parseo")
    void respuestaVaciaNoRompe() {
        when(restTemplate.postForObject(anyString(), any(), eq(GeorefDTOs.Respuesta.class)))
                .thenReturn(null);

        assertTrue(cliente.buscar(
                List.of(GeorefDTOs.Consulta.de("Av. Medrano 951", null, null))).isEmpty());
    }
}
