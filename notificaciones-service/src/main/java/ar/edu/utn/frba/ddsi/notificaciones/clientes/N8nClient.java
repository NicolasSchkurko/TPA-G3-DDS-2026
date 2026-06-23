package ar.edu.utn.frba.ddsi.notificaciones.clientes;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class N8nClient {
    @Value("${servicio.n8n.url}")
    private String n8nUrl;

    private final RestTemplate restTemplate;

    public N8nClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void enviarNotificacion(NotificacionPayload payload) {
        restTemplate.postForEntity(
                n8nUrl,
                payload,
                Void.class
        );
    }

}
