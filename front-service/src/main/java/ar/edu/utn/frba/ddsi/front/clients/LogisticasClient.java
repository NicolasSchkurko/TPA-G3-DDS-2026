package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Cliente REST para logisticas-service. Si el servicio no responde, devuelve valores
 * vacíos para que el front pueda degradar a modo demo en lugar de romperse.
 */
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

    /** Ítems de entrega del depósito. Ojo: es un listado global; filtrar por entidadDestino en el front. */
    public LogisticaDTOs.Bienes obtenerBienes() {
        try {
            return restClient.get()
                    .uri(baseUrl + "/entregas")
                    .retrieve()
                    .body(LogisticaDTOs.Bienes.class);
        } catch (Exception e) {
            log.error("Error al obtener entregas desde {}: {}", baseUrl, e.getMessage());
            return null;
        }
    }

    /** Bien por id de donación (el id de ítem de entrega ES el id de la donación). */
    public LogisticaDTOs.Bien obtenerBienPorDonacion(UUID idDonacion) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/entregas/{id}", idDonacion)
                    .retrieve()
                    .body(LogisticaDTOs.Bien.class);
        } catch (Exception e) {
            log.warn("No se encontró entrega para la donación {} o el servicio no respondió: {}", idDonacion, e.getMessage());
            return null;
        }
    }

    /** Confirmar (ENTREGADA, requiere fotoUrl) o rechazar (NO_RECIBIDA, requiere justificacion) una entrega. */
    public String actualizarEstadoEntrega(UUID idDonacion, LogisticaDTOs.ActualizacionEntrega dto) {
        try {
            String response = restClient.patch()
                    .uri(baseUrl + "/entregas/{id}/estado", idDonacion)
                    .body(dto)
                    .retrieve()
                    .body(String.class);
            return response != null ? response : "Estado actualizado correctamente.";
        } catch (Exception e) {
            log.error("Error al actualizar el estado de la entrega {}: {}", idDonacion, e.getMessage());
            throw e;
        }
    }

    /** Rutas generadas. El listado es global; filtrar por paradas[].direccion.idEntidad en el front. */
    public List<LogisticaDTOs.Ruta> obtenerRutas() {
        try {
            LogisticaDTOs.Rutas rutas = restClient.get()
                    .uri(baseUrl + "/rutas")
                    .retrieve()
                    .body(LogisticaDTOs.Rutas.class);
            return rutas == null || rutas.getRutas() == null ? List.of() : rutas.getRutas();
        } catch (Exception e) {
            log.error("Error al obtener rutas desde {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    /** Detalle de una ruta (para el mapa público de seguimiento por token/id). */
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
