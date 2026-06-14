package ar.edu.utn.frba.ddsi.incentivos.services;

//q los import los haga intellij, estoy en el celu

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service //o es @Component?
public class NotificacionService {
//cambiar a NotificacionClient
//cliente para consumir el servicio de notificaciones cuando actualicemosPerfil y se identifique necesidad de notificar

//en config podemos crear archivo para usar cliente gral a cualquier servicio y usar RestClient en vez de RestTemplate
    private final RestTemplate restTemplate;

    public NotificacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void enviarNotificacionDeAlgo(AlgoDTO dto) {
        restTemplate.postForEntity(
            "http://servicio-notificaciones/notificaciones",
            dto,
            Void.class
        );
    }
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