package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class DonacionesClient {

    private static final Logger log = LoggerFactory.getLogger(DonacionesClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public DonacionesClient(RestClient restClient,
                            @Value("${servicio.donaciones.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public List<DonacionDTO> obtenerTodasLasDonaciones() {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/donaciones")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DonacionDTO>>() {});
        } catch (Exception e) {
            log.error("Error al obtener donaciones desde {}: {}", baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    public DonacionDTO obtenerDonacionPorId(UUID id) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/donaciones/{id}", id)
                    .retrieve()
                    .body(DonacionDTO.class);
        } catch (Exception e) {
            log.error("Error al obtener donación con id {} desde {}: {}", id, baseUrl, e.getMessage());
            return null;
        }
    }

    public List<PersonaDonanteDTO> obtenerDonantes() {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/personas")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PersonaDonanteDTO>>() {});
        } catch (Exception e) {
            log.error("Error al obtener donantes desde {}: {}", baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }
}
