package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.front.services.ChoferPortalService;
import ar.edu.utn.frba.ddsi.front.services.IdentidadService;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
public class SeguimientoController {

    private final LogisticasClient logisticasClient;
    private final SeguimientoTokenService tokenService;
    private final ObjectMapper objectMapper;
    private final IdentidadService identidadService;
    private final ChoferPortalService choferPortalService;
    private final DonacionesClient donacionesClient;

    public SeguimientoController(LogisticasClient logisticasClient,
                                 SeguimientoTokenService tokenService,
                                 ObjectMapper objectMapper,
                                 IdentidadService identidadService,
                                 ChoferPortalService choferPortalService,
                                 DonacionesClient donacionesClient) {
        this.logisticasClient = logisticasClient;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.identidadService = identidadService;
        this.choferPortalService = choferPortalService;
        this.donacionesClient = donacionesClient;
    }

    @GetMapping("/seguimiento/{token}")
    public String seguimiento(@PathVariable String token, Authentication authentication, Model model) {
        UUID idRuta = tokenService.resolver(token).orElse(null);
        if (idRuta == null) {
            return "redirect:/";
        }

        JsonNode ruta = logisticasClient.obtenerRuta(idRuta);
        if (ruta == null || ruta.isNull()) {
            return "redirect:/";
        }

        if (!puedeVer(ruta, authentication)) {
            return "redirect:/sin-acceso";
        }

        String estado = texto(ruta, "estado");
        List<Map<String, Object>> paradas = paradasVista(ruta, estado);
        model.addAttribute("patente", texto(ruta.path("camionAsignado"), "patente"));
        model.addAttribute("estado", estado);
        model.addAttribute("estadoEtiqueta", etiquetaEstado(estado));
        model.addAttribute("hayTrazado", !puntos(ruta, estado).isEmpty());
        model.addAttribute("geojson", geoJson(ruta, estado));
        model.addAttribute("paradas", paradas);
        model.addAttribute("depositoEtiqueta", textoConDefault(ruta.path("deposito"), "etiqueta", "Depósito"));
        model.addAttribute("totalEntregadas", paradas.stream().mapToInt(p -> (int) p.getOrDefault("entregadas", 0)).sum());
        model.addAttribute("totalPaquetes", paradas.stream().mapToInt(p -> (int) p.getOrDefault("total", 0)).sum());
        return "seguimiento/ruta";
    }

    private boolean puedeVer(JsonNode ruta, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (tieneRol(authentication, "ADMIN")) {
            return true;
        }

        if (tieneRol(authentication, "CHOFER")) {
            UUID idChoferRuta = uuid(ruta.path("camionAsignado"), "idChofer");
            if (idChoferRuta != null) {
                ChoferDTO chofer = choferPortalService.choferActual(authentication).orElse(null);
                if (chofer != null && idChoferRuta.equals(chofer.getIdChofer())) {
                    return true;
                }
            }
        }

        if (tieneRol(authentication, "ENTIDAD")) {
            UUID idEntidad = identidadService.entidadIdActual(authentication).orElse(null);
            if (idEntidad != null && rutaContieneEntidad(ruta, idEntidad)) {
                return true;
            }
        }

        if (tieneRol(authentication, "DONANTE")) {
            try {
                UUID idDonante = identidadService.donanteIdActual(authentication).orElse(null);
                if (idDonante != null) {
                    Set<UUID> ids = new LinkedHashSet<>();
                    List<DonacionDTO> donaciones = donacionesClient.obtenerDonacionesDeDonante(idDonante);
                    if (donaciones != null) {
                        donaciones.stream()
                                .map(DonacionDTO::getId)
                                .filter(java.util.Objects::nonNull)
                                .forEach(ids::add);
                    }
                    if (rutaContieneDonaciones(ruta, ids)) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
                // Si el backend de donaciones no responde, no se concede acceso por este camino.
            }
        }

        return false;
    }

    private static boolean rutaContieneEntidad(JsonNode ruta, UUID idEntidad) {
        JsonNode paradas = ruta.path("paradas");
        if (!paradas.isArray()) return false;
        for (JsonNode parada : paradas) {
            if (idEntidad.equals(uuid(parada.path("direccion"), "idEntidad"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean rutaContieneDonaciones(JsonNode ruta, Set<UUID> idsDonaciones) {
        if (idsDonaciones.isEmpty()) return false;
        JsonNode paradas = ruta.path("paradas");
        if (!paradas.isArray()) return false;
        for (JsonNode parada : paradas) {
            JsonNode ids = parada.path("items").path("idsDonaciones");
            if (!ids.isArray()) continue;
            for (JsonNode id : ids) {
                if (!id.isTextual()) continue;
                try {
                    if (idsDonaciones.contains(UUID.fromString(id.asText()))) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Un id malformado en la ruta no debe romper el mapa.
                }
            }
        }
        return false;
    }

    private List<Map<String, Object>> paradasVista(JsonNode ruta, String estado) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        boolean enCurso = "EN_CURSO".equalsIgnoreCase(estado);

        JsonNode deposito = ruta.path("deposito");
        Map<String, Object> inicio = new LinkedHashMap<>();
        inicio.put("numero", 0);
        inicio.put("deposito", true);
        inicio.put("direccion", textoConDefault(deposito, "etiqueta", "Depósito"));
        inicio.put("estado", "deposito");
        inicio.put("estadoEtiqueta", "Punto de partida");
        inicio.put("entregadas", 0);
        inicio.put("total", 0);
        inicio.put("actual", false);
        resultado.add(inicio);

        JsonNode paradas = ruta.path("paradas");
        if (paradas.isArray()) {
            int numero = 1;
            boolean actualAsignada = false;
            for (JsonNode parada : paradas) {
                JsonNode direccion = parada.path("direccion");
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
                boolean completa = entregadas == total;
                boolean actual = enCurso && !completa && !actualAsignada;
                if (actual) actualAsignada = true;

                Map<String, Object> vista = new LinkedHashMap<>();
                vista.put("numero", numero);
                vista.put("deposito", false);
                vista.put("direccion", etiquetaDireccion(direccion));
                vista.put("estado", actual ? "actual" : (completa ? "completa" : "pendiente"));
                vista.put("estadoEtiqueta", actual ? "Actual" : (completa ? "Completada" : "Pendiente"));
                vista.put("entregadas", entregadas);
                vista.put("total", total);
                vista.put("actual", actual);
                resultado.add(vista);
                numero++;
            }
        }
        return resultado;
    }

    private List<Map<String, Object>> puntos(JsonNode ruta, String estado) {
        List<Map<String, Object>> puntos = new ArrayList<>();
        boolean enCurso = "EN_CURSO".equalsIgnoreCase(estado);

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
                boolean completa = entregadas == total;
                boolean actual = enCurso && !completa && !actualAsignada;
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

    private String geoJson(JsonNode ruta, String estado) {
        List<Map<String, Object>> puntos = puntos(ruta, estado);
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

        Map<String, Object> camion = puntoCamion(ruta, estado, puntos);
        if (camion != null) {
            features.add(feature("Point", camion.get("coordinates"), Map.class.cast(camion.get("properties"))));
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

    private Map<String, Object> puntoCamion(JsonNode ruta, String estado, List<Map<String, Object>> puntos) {
        if (!"EN_CURSO".equalsIgnoreCase(estado) || puntos.isEmpty()) {
            return null;
        }
        Map<String, Object> actual = null;
        for (Map<String, Object> punto : puntos) {
            Object props = punto.get("properties");
            if (props instanceof Map<?, ?> map && "actual".equals(map.get("estado"))) {
                actual = punto;
                break;
            }
        }
        if (actual == null) {
            actual = puntos.get(0);
        }
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("camion", true);
        propiedades.put("etiqueta", textoConDefault(ruta.path("camionAsignado"), "patente", "Camión"));
        Map<String, Object> punto = new LinkedHashMap<>();
        punto.put("coordinates", actual.get("coordinates"));
        punto.put("properties", propiedades);
        return punto;
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

    private static UUID uuid(JsonNode nodo, String campo) {
        String valor = texto(nodo, campo);
        if (valor.isBlank()) return null;
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    private static boolean tieneRol(Authentication authentication, String rol) {
        if (authentication == null) return false;
        String esperado = "ROLE_" + rol;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> esperado.equalsIgnoreCase(a.getAuthority()));
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
