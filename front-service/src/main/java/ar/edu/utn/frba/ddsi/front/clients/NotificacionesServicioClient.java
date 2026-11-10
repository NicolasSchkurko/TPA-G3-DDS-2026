package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.NotificacionServicioDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * Cliente REST para notificaciones-service. Si el servicio no responde, devuelve una lista
 * vacía para que la bandeja del portal siga funcionando con la actividad derivada.
 */
@Component
public class NotificacionesServicioClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesServicioClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public NotificacionesServicioClient(RestClient restClient,
                                        @Value("${servicio.notificaciones.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    /** Notificaciones más recientes de un destinatario (email o teléfono). Nunca lanza excepción. */
    public List<NotificacionServicioDTO> listarPorDestinatario(String destinatario, int size) {
        if (destinatario == null || destinatario.isBlank()) return Collections.emptyList();
        int limite = Math.min(Math.max(size, 1), 50);
        try {
            List<NotificacionServicioDTO> notificaciones = restClient.get()
                    .uri(baseUrl + "/notificaciones?destinatario={destinatario}&size={size}",
                            destinatario.trim().toLowerCase(), limite)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NotificacionServicioDTO>>() {});
            return notificaciones != null ? notificaciones : Collections.emptyList();
        } catch (Exception e) {
            log.warn("No se pudieron obtener las notificaciones de {}: {}", destinatario, e.getMessage());
            return Collections.emptyList();
        }
    }
}
