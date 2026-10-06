package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Recibe de la cola las notificaciones pendientes y las envía.
 *
 * <p><b>Le falta {@code @Component} y por eso no funcionaba.</b> Sin la anotación de
 * stereotypes la clase no es bean, Spring no la registra, y el {@code @RabbitListener} de
 * abajo nunca se procesa: la cola se llenaba y nadie consumía nada. Es el peor tipo de fallo
 * porque no rompe la compilación ni el arranque —el servicio levanta perfecto y solo recibe
 * mensajes que nadie lee—.
 */
@Component
public class ConsumidorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorNotificaciones.class);

    private final GestorNotificaciones gestor;
    private final RepositorioNotificaciones repositorioNotificaciones;

    public ConsumidorNotificaciones(GestorNotificaciones gestor,
                                    RepositorioNotificaciones repositorioNotificaciones) {
        this.gestor = gestor;
        this.repositorioNotificaciones = repositorioNotificaciones;
    }

    /**
     * Envía la notificación que llega del broker y deja el estado asentado.
     *
     * <p><b>El estado se persiste, que es lo que faltaba.</b> Se marcaba enviada o fallida
     * sobre el objeto pero nunca se guardaba, así que la base quedaba siempre en PENDIENTE:
     * el historial no registraba que la notificación había salido.
     *
     * <p><b>El fallo se registra y no se relanza</b>, a propósito. Si se relanzara, Spring
     * devolvería el mensaje a la cola y lo reintentaría en un ciclo cerrado. Para una
     * notificación a un teléfono o un mail que no se puede entregar, reintentar 5 veces
     * seguido es peor que dejarlo: lo que corresponde es un reintento con espera o una cola
     * de mensajes muertos, y eso todavía no está configurado.
     */
    @RabbitListener(queues = RabbitConfig.COLA_NOTIFICACIONES)
    public void recibir(Notificacion notificacion) {
        try {
            gestor.enviarNotificacion(
                    notificacion.getTipoMedioDeContacto(),
                    notificacion.getDireccionDeContacto(),
                    notificacion
            );

            notificacion.marcarEnviada();
        } catch (RuntimeException excepcion) {
            notificacion.marcarFallida();

            log.error("No se pudo enviar la notificación {}: {}",
                    notificacion.getId(), excepcion.getMessage(), excepcion);
        }

        repositorioNotificaciones.save(notificacion);
    }
}
