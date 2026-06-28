package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.NotificacionDTO;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificacionesClient {
    private final RestTemplate restTemplate;

    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    public NotificacionesClient(RestTemplate restTemplate) {
      this.restTemplate = restTemplate;
    }

    public Void enviarNotificacion(NotificacionDTO dto) {
        restTemplate.postForEntity(
                notificacionesUrl,
                dto,
                Void.class
        );
        return null;
    }
}








