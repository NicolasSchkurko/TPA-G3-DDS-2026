package ar.edu.utn.frba.ddsi.incentivos.services;

//q los import los haga intellij, estoy en el celu

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class NotificacionService {
    private final RestClient generalClient;

    public NotificacionService(RestClient generalClient) {
        this.generalClient = generalClient;
    }

    public void enviarNotificacion(Perfil perfil) {
        //TODO crear notificacion a partir de perfil
        //TODO dto a enviar
        generalClient.post()
                .uri("http://localhost:8082/notificaciones/dtoAEnviar", dtoAEnviar)
                .retrieve()
                .body(void.class);
    }
}