package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.front.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.front.dto.PageResponse;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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
    private final RestClient clientePatch;
    private final String baseUrl;

    public LogisticasClient(RestClient restClient,
                            @Value("${servicio.logisticas.url}") String baseUrl) {
        this.restClient = restClient;
        // El RestClient de la aplicación usa SimpleClientHttpRequestFactory (HttpURLConnection),
        // que no soporta PATCH. Usamos JdkClientHttpRequestFactory para los PATCH.
        this.clientePatch = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
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

    /** Bien por id de donación (el id de ítem de entrega ES el id de la donación). REVISAR*/
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
            String response = clientePatch.patch()
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

    /** Camiones de la flota. Devuelve lista vacía si el servicio no responde. */
    public List<CamionDTO> obtenerCamiones() {
        try {
            PageResponse<CamionDTO> page = restClient.get()
                    .uri(baseUrl + "/camiones?page=0&size=200")
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<CamionDTO>>() {});
            return page != null && page.getContent() != null ? page.getContent() : List.of();
        } catch (Exception e) {
            log.error("Error al obtener camiones desde {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    /** Rutas generadas. El listado es global; filtrar por chofer o paradas[].direccion.idEntidad en el front. */
    public List<LogisticaDTOs.Ruta> obtenerRutas() {
        try {
            PageResponse<LogisticaDTOs.Ruta> page = restClient.get()
                    .uri(baseUrl + "/rutas")
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<LogisticaDTOs.Ruta>>() {});
            return page != null && page.getContent() != null ? page.getContent() : List.of();
        } catch (Exception e) {
            log.error("Error al obtener rutas desde {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    /** Choferes de la flota. Devuelve lista vacía si el servicio no responde. */
    public List<ChoferDTO> obtenerChoferes() {
        try {
            PageResponse<ChoferDTO> page = restClient.get()
                    .uri(baseUrl + "/choferes?page=0&size=200")
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<ChoferDTO>>() {});
            return page != null && page.getContent() != null ? page.getContent() : List.of();
        } catch (Exception e) {
            log.error("Error al obtener choferes desde {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    /** Ruta activa (en curso o programada) del chofer; null si no tiene. */
    public LogisticaDTOs.Ruta obtenerRutaActivaDelChofer(UUID idChofer) {
        try {
            var response = restClient.get()
                    .uri(baseUrl + "/rutas/chofer/{id}/activa", idChofer)
                    .retrieve()
                    .toEntity(LogisticaDTOs.Ruta.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error al obtener la ruta activa del chofer {}: {}", idChofer, e.getMessage());
            return null;
        }
    }

    /** Historial paginado de rutas del chofer. Devuelve una página vacía si falla. */
    public PageResponse<LogisticaDTOs.Ruta> obtenerHistorialDelChofer(UUID idChofer, int page, int size) {
        try {
            PageResponse<LogisticaDTOs.Ruta> resultado = restClient.get()
                    .uri(baseUrl + "/rutas/chofer/{id}?page={page}&size={size}", idChofer, page, size)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<LogisticaDTOs.Ruta>>() {});
            return resultado != null ? resultado : new PageResponse<>();
        } catch (Exception e) {
            log.error("Error al obtener el historial del chofer {}: {}", idChofer, e.getMessage());
            return new PageResponse<>();
        }
    }

    /** El chofer informa el inicio de su ruta programada. */
    public void iniciarRuta(UUID idChofer) {
        try {
            clientePatch.patch()
                    .uri(baseUrl + "/rutas/chofer/{id}/iniciar", idChofer)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Error al iniciar la ruta del chofer {}: {}", idChofer, e.getMessage());
            throw e;
        }
    }

    /** El chofer finaliza su ruta activa; las entregas no confirmadas vuelven a depósito. */
    public void terminarRuta(UUID idChofer) {
        try {
            clientePatch.patch()
                    .uri(baseUrl + "/rutas/chofer/{id}/terminar", idChofer)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Error al terminar la ruta del chofer {}: {}", idChofer, e.getMessage());
            throw e;
        }
    }

    /** Detalle crudo de una ruta; lo consume la vista pública de seguimiento. */
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
