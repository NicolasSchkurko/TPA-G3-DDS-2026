package ar.edu.utn.frba.ddsi.donaciones.services;
import ar.edu.utn.frba.ddsi.donaciones.dto.MediosContactoDTO;
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

    // esto no sabemos si esta bien, hay q plantearlo mejor
    public void enviarNotificacion (MediosContactoDTO.NotificacionDTO dto) {

      restTemplate.getForObject(
          notificacionesUrl,
          MediosContactoDTO.NotificacionDTO.class
      );
    }


}








