package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorNotificaciones;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class ConsumidorNotificaciones {
    private final GestorNotificaciones gestor;

    public ConsumidorNotificaciones(GestorNotificaciones gestor) {
        this.gestor = gestor;
    }

    @RabbitListener(queues = RabbitConfig.COLA_NOTIFICACIONES)
    public void recibir(Notificacion notificacion) {

        try {
            gestor.enviarNotificacion(
                    notificacion.getTipoMedioDeContacto(),
                    notificacion.getDireccionDeContacto(),
                    notificacion
            );

            notificacion.marcarEnviada();

        } catch (Exception e) {

            notificacion.marcarFallida();
        }
    }
}
