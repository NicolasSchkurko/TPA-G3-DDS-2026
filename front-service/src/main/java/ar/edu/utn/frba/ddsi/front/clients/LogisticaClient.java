package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

public class LogisticaClient {
    private static final Logger log = LoggerFactory.getLogger(LogisticaClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public LogisticaClient(RestClient restClient,
                           @Value("${servicio.logistica.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public List<CamionDTO> obtenerTodosLoscamiones() {
        try {
            PageResponse<CamionDTO> page = restClient.get()
                    .uri(baseUrl + "/camiones")
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<CamionDTO>>() {});
            return page != null ? page.getContent() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al obtener camiones desde {}: {}", baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }
}
