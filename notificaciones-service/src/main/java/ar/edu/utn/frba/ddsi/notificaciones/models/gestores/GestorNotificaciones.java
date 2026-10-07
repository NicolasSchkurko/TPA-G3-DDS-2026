package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;

/** Guarda las solicitudes de notificación y las publica para que el consumidor las despache. */
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

    @Transactional
    public void enviarSolicitudDeNotificacion(String tipoMedioDeContacto,
                                              String direccionDeContacto,
                                              String asunto,
                                              String cuerpo) {
        Notificacion notificacion =
                crearNotificacion(tipoMedioDeContacto, direccionDeContacto, asunto, cuerpo);

        // En afterCommit: antes del commit el consumidor no ve la fila, y si el broker falla el
        // commit ya hecho deja la notificación PENDIENTE para reintentar.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productorNotificaciones.enviar(notificacion);
            }
        });
    }

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

    public void enviarNotificacion(String tipoMedioDeContacto,
                                   String direccionContacto,
                                   Notificacion notificacion) {
        try {
            // La dirección del mensaje gana; los medios leen la de la entidad.
            if (direccionContacto != null && !direccionContacto.isBlank()) {
                notificacion.setDireccionDeContacto(direccionContacto);
            }

            MedioDeEnvio medioDeContacto = factory.mapearAMedioEnvio(tipoMedioDeContacto);
            medioDeContacto.enviarNotificacion(notificacion);
        } catch (RuntimeException excepcion) {
            notificacion.marcarFallida();
            throw new IllegalArgumentException(mensajeDeEnvioFallido(excepcion), excepcion);
        }
    }

    private static String mensajeDeEnvioFallido(RuntimeException excepcion) {
        String detalle = excepcion.getMessage();

        return detalle == null
                ? "Ocurrió un problema inesperado al enviar la notificación"
                : "Ocurrió un problema inesperado al enviar la notificación: " + detalle;
    }

    public Optional<Notificacion> obtenerNotificacionPorId(UUID id) {
        return repositorioNotificaciones.findById(id);
    }
}
