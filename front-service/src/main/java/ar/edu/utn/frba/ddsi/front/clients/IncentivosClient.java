package ar.edu.utn.frba.ddsi.front.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
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
}
