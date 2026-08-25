package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionPayload;
import ar.edu.utn.frba.ddsi.notificaciones.gateways.NotificacionGateway;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.stereotype.Component;

@Component("Mail")
public class Mail extends MedioDeEnvio {
    public Mail(NotificacionGateway gateway) {
        super(gateway);
    }

    @Override
    public void enviarNotificacion(Notificacion notificacion) {
        NotificacionPayload payload = new NotificacionPayload(
                "email",
                notificacion.getDireccionDeContacto(),
                notificacion.getMensaje()
        );

        gateway.enviar(payload);
    }

}
