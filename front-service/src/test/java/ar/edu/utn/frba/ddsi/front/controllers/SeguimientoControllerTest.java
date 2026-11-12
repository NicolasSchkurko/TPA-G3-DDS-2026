package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Seguimiento de ruta en el front")
class SeguimientoControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Une las coordenadas de las paradas en un LineString y agrega un punto por parada")
    void armaElGeojsonDesdeLasParadas() throws Exception {
        UUID idRuta = UUID.randomUUID();
        JsonNode ruta = objectMapper.readTree("""
                {
                  "idRuta": "%s",
                  "paradas": [
                    {"direccion": {"latitud": -34.60, "longitud": -58.38}},
                    {"direccion": {"latitud": -34.61, "longitud": -58.42}},
                    {"direccion": {"latitud": null, "longitud": null}}
                  ]
                }
                """.formatted(idRuta));

        LogisticasClient cliente = mock(LogisticasClient.class);
        when(cliente.obtenerRuta(idRuta)).thenReturn(ruta);

        ExtendedModelMap model = new ExtendedModelMap();
        String vista = new SeguimientoController(cliente, objectMapper)
                .seguimiento(idRuta, model);

        assertThat(vista).isEqualTo("seguimiento/ruta");
        assertThat(model.get("hayTrazado")).isEqualTo(true);

        JsonNode geojson = objectMapper.readTree((String) model.get("geojson"));
        assertThat(geojson.path("type").asText()).isEqualTo("FeatureCollection");
        assertThat(geojson.path("features").size()).isEqualTo(3);
        assertThat(geojson.path("features").get(0).path("geometry").path("type").asText())
                .isEqualTo("LineString");
        assertThat(geojson.path("features").get(0).path("geometry").path("coordinates").get(0).get(0).asDouble())
                .isEqualTo(-58.38);
        assertThat(geojson.path("features").get(0).path("geometry").path("coordinates").get(0).get(1).asDouble())
                .isEqualTo(-34.60);
    }

    @Test
    @DisplayName("Una ruta inexistente deja el modelo sin trazado, sin romper la vista")
    void rutaInexistenteNoRompe() throws Exception {
        UUID idRuta = UUID.randomUUID();
        LogisticasClient cliente = mock(LogisticasClient.class);
        when(cliente.obtenerRuta(idRuta)).thenReturn(null);

        ExtendedModelMap model = new ExtendedModelMap();
        new SeguimientoController(cliente, objectMapper).seguimiento(idRuta, model);

        assertThat(model.get("hayTrazado")).isEqualTo(false);
        JsonNode geojson = objectMapper.readTree((String) model.get("geojson"));
        assertThat(geojson.path("features").size()).isZero();
    }
}
