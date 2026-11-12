package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoTokenService;
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
    private final SeguimientoTokenService tokenService;
    private final ObjectMapper objectMapper;

    public SeguimientoController(LogisticasClient logisticasClient,
                                 SeguimientoTokenService tokenService,
                                 ObjectMapper objectMapper) {
        this.logisticasClient = logisticasClient;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/seguimiento/{token}")
    public String seguimiento(@PathVariable String token, Model model) {
        UUID idRuta = tokenService.resolver(token).orElse(null);
        if (idRuta == null) {
            return "redirect:/";
        }

        JsonNode ruta = logisticasClient.obtenerRuta(idRuta);
        if (ruta == null || ruta.isNull()) {
            return "redirect:/";
        }

        String estado = texto(ruta, "estado");
        model.addAttribute("patente", texto(ruta.path("camionAsignado"), "patente"));
        model.addAttribute("estadoEtiqueta", etiquetaEstado(estado));
        model.addAttribute("hayTrazado", !puntos(ruta).isEmpty());
        model.addAttribute("geojson", geoJson(ruta));

        return "seguimiento/ruta";
    }

    private List<Map<String, Object>> puntos(JsonNode ruta) {
        List<Map<String, Object>> puntos = new ArrayList<>();

        JsonNode deposito = ruta.path("deposito");
        if (tieneCoordenadas(deposito)) {
            Map<String, Object> propiedades = new LinkedHashMap<>();
            propiedades.put("deposito", true);
            propiedades.put("etiqueta", textoConDefault(deposito, "etiqueta", "Depósito"));
            puntos.add(punto(deposito, propiedades));
        }

        JsonNode paradas = ruta.path("paradas");
        if (paradas.isArray()) {
            int numero = 1;
            boolean actualAsignada = false;
            for (JsonNode parada : paradas) {
                JsonNode direccion = parada.path("direccion");
                if (!tieneCoordenadas(direccion)) {
                    numero++;
                    continue;
                }
                int total = 0;
                int entregadas = 0;
                JsonNode bienes = parada.path("items").path("bienes");
                if (bienes.isArray()) {
                    for (JsonNode bien : bienes) {
                        total++;
                        if ("ENTREGADA".equalsIgnoreCase(texto(bien, "estado"))) {
                            entregadas++;
                        }
                    }
                }
                boolean completa = total > 0 && entregadas == total;
                boolean actual = !completa && !actualAsignada;
                if (actual) actualAsignada = true;

                Map<String, Object> propiedades = new LinkedHashMap<>();
                propiedades.put("parada", numero);
                propiedades.put("estado", actual ? "actual" : (completa ? "completa" : "pendiente"));
                propiedades.put("direccion", etiquetaDireccion(direccion));
                propiedades.put("entregadas", entregadas);
                propiedades.put("total", total);
                puntos.add(punto(direccion, propiedades));
                numero++;
            }
        }
        return puntos;
    }

    private String geoJson(JsonNode ruta) {
        List<Map<String, Object>> puntos = puntos(ruta);
        List<Map<String, Object>> features = new ArrayList<>();

        List<List<Double>> linea = new ArrayList<>();
        for (Map<String, Object> punto : puntos) {
            @SuppressWarnings("unchecked")
            List<Double> coords = (List<Double>) punto.get("coordinates");
            linea.add(coords);
        }
        if (linea.size() >= 2) {
            features.add(feature("LineString", linea, Map.of()));
        }
        for (Map<String, Object> punto : puntos) {
            features.add(feature("Point", punto.get("coordinates"), Map.class.cast(punto.get("properties"))));
        }

        Map<String, Object> coleccion = new LinkedHashMap<>();
        coleccion.put("type", "FeatureCollection");
        coleccion.put("features", features);
        try {
            return objectMapper.writeValueAsString(coleccion);
        } catch (Exception e) {
            return "{\"type\":\"FeatureCollection\",\"features\":[]}";
        }
    }

    private static Map<String, Object> punto(JsonNode direccion, Map<String, Object> propiedades) {
        Map<String, Object> punto = new LinkedHashMap<>();
        punto.put("coordinates", List.of(direccion.path("longitud").asDouble(), direccion.path("latitud").asDouble()));
        punto.put("properties", propiedades);
        return punto;
    }

    private static Map<String, Object> feature(String tipo, Object coordenadas, Map<String, Object> propiedades) {
        Map<String, Object> geometria = new LinkedHashMap<>();
        geometria.put("type", tipo);
        geometria.put("coordinates", coordenadas);

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometria);
        feature.put("properties", propiedades != null ? propiedades : Map.of());
        return feature;
    }

    private static boolean tieneCoordenadas(JsonNode nodo) {
        return nodo != null && nodo.path("latitud").isNumber() && nodo.path("longitud").isNumber();
    }

    private static String etiquetaDireccion(JsonNode direccion) {
        String calle = texto(direccion, "calleUno");
        String altura = direccion.path("altura").isNumber() ? String.valueOf(direccion.path("altura").asInt()) : "";
        String base = (calle + " " + altura).trim();
        if (!base.isBlank()) return base;
        String ciudad = texto(direccion, "ciudad");
        return ciudad.isBlank() ? "Dirección no informada" : ciudad;
    }

    private static String texto(JsonNode nodo, String campo) {
        if (nodo == null) return "";
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() ? "" : valor.asText("");
    }

    private static String textoConDefault(JsonNode nodo, String campo, String defecto) {
        String valor = texto(nodo, campo);
        return valor.isBlank() ? defecto : valor;
    }

    private static String etiquetaEstado(String estado) {
        if (estado == null || estado.isBlank()) return "Sin estado";
        return switch (estado.toUpperCase()) {
            case "PROGRAMADA" -> "Programada";
            case "EN_CURSO" -> "En curso";
            case "FINALIZADA" -> "Finalizada";
            default -> estado;
        };
    }
}
