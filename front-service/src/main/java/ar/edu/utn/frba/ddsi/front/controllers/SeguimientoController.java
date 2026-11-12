package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class SeguimientoController {

    private final LogisticasClient logisticasClient;
    private final ObjectMapper objectMapper;

    public SeguimientoController(LogisticasClient logisticasClient, ObjectMapper objectMapper) {
        this.logisticasClient = logisticasClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/seguimiento/{idRuta}")
    public String seguimiento(@PathVariable UUID idRuta, Model model) {
        JsonNode ruta = logisticasClient.obtenerRuta(idRuta);
        List<List<Double>> coordenadas = coordenadas(ruta);

        model.addAttribute("idRuta", idRuta);
        model.addAttribute("ruta", ruta);
        model.addAttribute("hayTrazado", coordenadas.size() >= 2);
        model.addAttribute("geojson", geoJson(coordenadas));

        return "seguimiento/ruta";
    }

    private List<List<Double>> coordenadas(JsonNode ruta) {
        List<List<Double>> coordenadas = new ArrayList<>();
        if (ruta == null || !ruta.path("paradas").isArray()) {
            return coordenadas;
        }

        for (JsonNode parada : ruta.path("paradas")) {
            JsonNode direccion = parada.path("direccion");
            JsonNode latitud = direccion.path("latitud");
            JsonNode longitud = direccion.path("longitud");
            if (latitud.isNumber() && longitud.isNumber()) {
                coordenadas.add(List.of(longitud.asDouble(), latitud.asDouble()));
            }
        }

        return coordenadas;
    }

    private String geoJson(List<List<Double>> coordenadas) {
        List<Map<String, Object>> features = new ArrayList<>();

        if (coordenadas.size() >= 2) {
            features.add(feature("LineString", coordenadas, null));
        }

        for (int i = 0; i < coordenadas.size(); i++) {
            features.add(feature("Point", coordenadas.get(i), i + 1));
        }

        Map<String, Object> coleccion = new LinkedHashMap<>();
        coleccion.put("type", "FeatureCollection");
        coleccion.put("features", features);

        try {
            return objectMapper.writeValueAsString(coleccion);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el trazado de la ruta", e);
        }
    }

    private Map<String, Object> feature(String tipo, Object coordenadas, Integer numeroParada) {
        Map<String, Object> geometria = new LinkedHashMap<>();
        geometria.put("type", tipo);
        geometria.put("coordinates", coordenadas);

        Map<String, Object> propiedades = new LinkedHashMap<>();
        if (numeroParada != null) {
            propiedades.put("parada", numeroParada);
        }

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometria);
        feature.put("properties", propiedades);
        return feature;
    }
}
