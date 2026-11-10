package ar.edu.utn.frba.ddsi.front.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class LogisticasClient {

    private static final Logger log = LoggerFactory.getLogger(LogisticasClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public LogisticasClient(RestClient restClient,
                            @Value("${servicio.logisticas.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public JsonNode obtenerRuta(UUID idRuta) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/rutas/{id}", idRuta)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.error("Error al obtener la ruta {} desde {}: {}", idRuta, baseUrl, e.getMessage());
            return null;
        }
    }
}
