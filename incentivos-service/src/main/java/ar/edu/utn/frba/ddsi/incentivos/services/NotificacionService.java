package ar.edu.utn.frba.ddsi.incentivos.services;

//q los import los haga intellij, estoy en el celu

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class NotificacionService {
    private final RestClient generalClient;

    public NotificacionService(RestClient generalClient) {
        this.generalClient = generalClient;
    }

    public PerfilNotificacionDTO obtenerContactoDeNotificacionesPorId(UUID id) {
        return generalClient.post()
            .uri("http://localhost:8082/notificaciones/{id}", id)
            .retrieve()
            .body(PerfilNotificacionDTO.class);
    }
}
