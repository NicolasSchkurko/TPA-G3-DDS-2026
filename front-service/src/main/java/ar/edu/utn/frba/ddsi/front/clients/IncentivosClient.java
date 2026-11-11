package ar.edu.utn.frba.ddsi.front.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class IncentivosClient {

    private static final Logger log = LoggerFactory.getLogger(IncentivosClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public IncentivosClient(RestClient restClient,
                            @Value("${servicio.incentivos.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> obtenerPerfilPublico(String idUsuario) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/perfiles/{idUsuario}/publico", idUsuario)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error al obtener perfil público {} desde {}: {}", idUsuario, baseUrl, e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> obtenerMision(String idUsuario) {
        return obtenerMapa("/api/perfiles/" + idUsuario + "/mision", "misión");
    }

    public List<Map<String, Object>> obtenerInsignias(String idUsuario) {
        try {
            Map<String, Object> pagina = restClient.get()
                    .uri(baseUrl + "/api/perfiles/{idUsuario}/insignias?page=0&size=100", idUsuario)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            Object content = pagina == null ? null : pagina.get("content");
            if (content instanceof List<?> elementos) {
                return elementos.stream()
                        .filter(Map.class::isInstance)
                        .map(elemento -> (Map<String, Object>) elemento)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("No se pudieron obtener insignias del perfil {}: {}", idUsuario, e.getMessage());
        }
        return Collections.emptyList();
    }

    public Map<String, Object> obtenerPuestoRanking(String idUsuario) {
        return obtenerMapa("/api/rankings/" + idUsuario + "/puestoRanking", "puesto del ranking");
    }

    public Map<String, Object> obtenerRankingActual() {
        return obtenerMapa("/api/rankings/actual", "ranking mensual");
    }

    private Map<String, Object> obtenerMapa(String path, String descripcion) {
        try {
            Map<String, Object> respuesta = restClient.get()
                    .uri(baseUrl + path)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return respuesta == null ? Collections.emptyMap() : respuesta;
        } catch (Exception e) {
            log.warn("No se pudo obtener {} desde {}: {}", descripcion, baseUrl, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
