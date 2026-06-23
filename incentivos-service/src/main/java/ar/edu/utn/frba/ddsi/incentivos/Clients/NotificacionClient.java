package ar.edu.utn.frba.ddsi.incentivos.Clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificacionClient {
//cliente para consumir el servicio de notificaciones cuando actualicemosPerfil y se identifique necesidad de notificar
    @Value("${servicio.notificaciones.url}")
    private String notificacionesUrl;

    private final RestTemplate restTemplate;

    public NotificacionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void enviarNotificacion(PerfilNotificacionDTO dto) throws EnvioNotificacionException {
        try {
            restTemplate.postForEntity(
                    notificacionesUrl,
                    dto,
                    void.class
            );
        } catch (Exception e) {
            throw new EnvioNotificacionException(dto);
        }
    }
}
