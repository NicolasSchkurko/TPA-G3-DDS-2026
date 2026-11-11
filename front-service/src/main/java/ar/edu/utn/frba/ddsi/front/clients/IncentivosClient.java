package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.DonacionDestacadaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /** Baja lógica e idempotente del perfil en incentivos. */
    public void eliminarPerfil(UUID idUsuario) {
        restClient.delete()
                .uri(baseUrl + "/api/perfiles/sincronizacion/{idUsuario}", idUsuario)
                .retrieve()
                .toBodilessEntity();
    }

    /** Últimas donaciones entregadas que completaron una misión. Nunca propaga la falla. */
    public List<DonacionDestacadaDTO> obtenerDonacionesDestacadas() {
        try {
            List<DonacionDestacadaDTO> destacadas = restClient.get()
                    .uri(baseUrl + "/api/donaciones/destacadas")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DonacionDestacadaDTO>>() {});
            return destacadas != null ? destacadas : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al obtener donaciones destacadas desde {}: {}",
                    baseUrl, e.getMessage());
            return Collections.emptyList();
        }
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
}
