package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Recibe las solicitudes de notificación y las publica en la cola.
 *
 * <p><b>El procesamiento no es acá.</b> Antes este gestor tenía un
 * {@link java.util.concurrent.BlockingQueue} en memoria y un método {@code @Scheduled} que
 * la vaciaba cada dos segundos. El cambio a RabbitMQ reemplazó esa cola por el broker, y el
 * código se actualizó a medias: el campo {@code cola} se fue pero quedaron las llamadas a
 * {@code cola.add} y {@code cola.poll}, y el import de {@code @Scheduled} se quitó sin
 * borrar el método que lo usaba. Dejaba el módulo entero sin compilar.
 *
 * <p>Ahora el productor publica y {@code ConsumidorNotificaciones} recibe del broker. No
 * queda un segundo consumidor en memoria: si quedara, la misma notificación se intentaría
 * enviar dos veces por dos caminos distintos.
 */
@Service
public class GestorNotificaciones {

    private final RepositorioNotificaciones repositorioNotificaciones;
    private final MedioDeEnvioFactory factory;
    private final ProductorNotificaciones productorNotificaciones;

    public GestorNotificaciones(RepositorioNotificaciones repositorioNotificaciones,
                               MedioDeEnvioFactory factory,
                               ProductorNotificaciones productorNotificaciones) {
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.factory = factory;
        this.productorNotificaciones = productorNotificaciones;
    }

    /**
     * Guarda la notificación y la publica para que el consumidor la envíe.
     *
     * <p><b>La transacción es la que hace que el orden sirva.</b> Con @Transactional, el
     * save() solo encola el INSERT y la publicación ocurre antes del commit. El
     * consumidor corre en otra transacción, así que al recibir el mensaje todavía no
     * ve la fila: fallaba con EntityNotFoundException al buscar la notificación por id.
     * Con la transacción, el commit ocurre al salir del método y recién ahí el
     * mensaje llega a un registro que ya existe.
     *
     * <p><b>Guardar antes de publicar evita perder el aviso.</b> Si la publicación falla,
     * queda el registro en PENDIENTE y se puede reintentar desde la base; al revés no hay
     * forma de saber que la notificación existió.
     */
    @Transactional
    public void enviarSolicitudDeNotificacion(String tipoMedioDeContacto,
                                              String direccionDeContacto,
                                              String asunto,
                                              String cuerpo) {
        Notificacion notificacion =
                crearNotificacion(tipoMedioDeContacto, direccionDeContacto, asunto, cuerpo);

        notificacion.marcarPendiente();
        repositorioNotificaciones.save(notificacion);

        // Si esto tira, la notificación queda en PENDIENTE en la base y se puede
        // reintentar desde ahí. Es el motivo de guardar primero.
        productorNotificaciones.enviar(notificacion);
    }

    /**
     * Arma la notificación con estado PENDIENTE y la guarda.
     *
     * <p>Guarda adentro porque así la usan los dos llamadores por igual. El estado se setea
     * en el constructor, así que el {@code marcarPendiente()} del flujo de envío es
     * redundante y quedó solo.
     */
    public Notificacion crearNotificacion(String tipoMedioDeContacto,
                                          String direccionDeContacto,
                                          String asunto,
                                          String cuerpo) {
        Notificacion notificacion = new Notificacion(
                direccionDeContacto,
                tipoMedioDeContacto,
                new Mensaje(asunto, cuerpo)
        );

        return repositorioNotificaciones.save(notificacion);
    }

    /**
     * Envía la notificación por el medio que corresponda.
     *
     * <p>No persiste el estado: eso es responsabilidad del consumidor, que es quien sabe si
     * la publicación por el broker funcionó.
     */
    public void enviarNotificacion(String tipoMedioContacto,
                                   String direccionContacto,
                                   Notificacion notificacion) {
        try {
            MedioDeEnvio medioDeContacto = factory.mapearAMedioEnvio(tipoMedioContacto);
            medioDeContacto.enviarNotificacion(notificacion);
        } catch (RuntimeException excepcion) {
            notificacion.marcarFallida();

            if (excepcion.getMessage() != null) {
                throw new IllegalArgumentException(
                        "Ocurrió un problema inesperado al enviar la notificación: "
                                + excepcion.getMessage(), excepcion);
            }
            throw new IllegalArgumentException(
                    "Ocurrió un problema inesperado al enviar la notificación", excepcion);
        }
    }

    public Optional<Notificacion> obtenerNotificacionPorId(UUID id) {
        return repositorioNotificaciones.findById(id);
    }
}
