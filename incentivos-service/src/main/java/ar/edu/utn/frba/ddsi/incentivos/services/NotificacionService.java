package ar.edu.utn.frba.ddsi.incentivos.services;

//q los import los haga intellij, estoy en el celu

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@Value("${servicio.notificaciones.url}")
public class NotificacionService {
//cliente para consumir el servicio de notificaciones cuando actualicemosPerfil y se identifique necesidad de notificar

    private String donacionesUrl;
    public void enviarNotificacion(PerfilNotificacionDTO dto) {
        restTemplate.postForEntity(
            "http://servicio-notificaciones/notificaciones",
            dto,
            Void.class
        );
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
}