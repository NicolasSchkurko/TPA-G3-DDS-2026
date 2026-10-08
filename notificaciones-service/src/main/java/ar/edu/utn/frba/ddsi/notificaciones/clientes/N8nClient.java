package ar.edu.utn.frba.ddsi.notificaciones.clientes;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class N8nClient {

    private static final Logger log = LoggerFactory.getLogger(N8nClient.class);

    @Value("${servicio.n8n.url}")
    private String n8nUrl;

    private final RestTemplate restTemplate;

    public N8nClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void enviarNotificacion(NotificacionPayload payload) {
        String asunto = payload.getMensaje() != null ? payload.getMensaje().getAsunto() : null;

        log.info("[N8N] -> POST {} | canal={} | destino={} | asunto={} | cuerpo={}",
                n8nUrl, payload.getCanal(), payload.getDireccionContacto(), asunto,
                payload.getMensaje() != null ? payload.getMensaje().getCuerpo() : null);

        try {
            ResponseEntity<Void> respuesta = restTemplate.postForEntity(n8nUrl, payload, Void.class);
            log.info("[N8N] <- OK {} | canal={} | destino={}",
                    respuesta.getStatusCode(), payload.getCanal(), payload.getDireccionContacto());
        } catch (RestClientException error) {
            log.error("[N8N] <- ERROR llamando a {} | canal={} | destino={}: {}",
                    n8nUrl, payload.getCanal(), payload.getDireccionContacto(), error.getMessage(), error);
            throw error;
        }
    }
}
