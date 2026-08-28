package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificacionesClient {
    private final RestTemplate restTemplate;

    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    public NotificacionesClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Void enviarNotificacion(NotificacionDTO dto) {
        try {
            restTemplate.postForEntity(notificacionesUrl, dto, Void.class);
            return null;
        } catch (Exception e) {
            System.err.println("Fallo al enviar notificación: " + e.getMessage());
            return null;
        }
    }
}