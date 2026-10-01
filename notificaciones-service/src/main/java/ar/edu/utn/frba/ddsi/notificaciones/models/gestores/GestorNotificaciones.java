package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GestorNotificaciones {
    private final RepositorioNotificaciones repositorioNotificaciones;
    private final MedioDeEnvioFactory factory;
    private final ProductorNotificaciones productorNotificaciones;

    public GestorNotificaciones(RepositorioNotificaciones repositorioNotificaciones, MedioDeEnvioFactory factory, ProductorNotificaciones productorNotificaciones) {
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.factory = factory;
        this.productorNotificaciones = productorNotificaciones;
    }

    public void enviarSolicitudDeNotificacion(String tipoMedioDeContacto, String direccionDeContacto, String asunto, String cuerpo) {

        Notificacion notificacion = crearNotificacion(tipoMedioDeContacto, direccionDeContacto, asunto, cuerpo);
        notificacion.marcarPendiente();
        repositorioNotificaciones.guardar(notificacion);
        productorNotificaciones.enviar(notificacion);

    }

    // Crea una Notificacion a partir de una SolicitudNotificacion y la guarda en el repositorio
    public Notificacion crearNotificacion(String tipoMedioDeContacto, String direccionDeContacto, String asunto, String cuerpo) {

        Mensaje mensaje = new Mensaje(asunto, cuerpo);
        Notificacion notificacion = new Notificacion(direccionDeContacto, tipoMedioDeContacto, mensaje);

        return notificacion;
    }


    // Por ahora solo envia al medio predeterminado
    public void enviarNotificacion(String tipoMedioContacto, String direccionContacto, Notificacion notificacion) {

        try {
            MedioDeEnvio medioDeContacto = factory.mapearAMedioEnvio(tipoMedioContacto);
            medioDeContacto.enviarNotificacion(notificacion);
            notificacion.marcarEnviada();

        } catch (IllegalArgumentException ex) {

            notificacion.marcarFallida();

            if (ex.getMessage() != null) {
                throw new IllegalArgumentException("Ocurrió un problema inesperado al enviar la notificación: " + ex.getMessage(), ex);
            }
            throw new IllegalArgumentException("Ocurrio un problema inesperado al enviar la notificacion", ex);
        }
    }

    public Optional<Notificacion> obtenerNotificacionPorId(UUID id) {
        return repositorioNotificaciones.findById(id);
    }
}
