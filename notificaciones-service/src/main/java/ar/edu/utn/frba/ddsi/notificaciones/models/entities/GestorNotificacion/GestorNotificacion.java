package ar.edu.utn.frba.ddsi.notificaciones.models.entities.GestorNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.ErrorAlEnviarNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.SolicitudNotificacion.SolicitudNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;

/**
 * Coordina el ciclo de una notificacion: la crea desde una solicitud, selecciona
 * el medio de envio correspondiente, actualiza su estado y la registra en el repositorio.
 */
public class GestorNotificacion {
    private final MedioDeEnvioFactory medioDeEnvioFactory;
    private final RepositorioNotificaciones repositorioNotificaciones;

    /*
     * Estas dependencias se reciben por constructor para dejar explicito que el gestor
     * necesita una factory de medios y un repositorio. Asi evitamos que busque singletons
     * globales por su cuenta, lo que haria mas dificil testearlo o cambiar implementaciones.
     */
    public GestorNotificacion(MedioDeEnvioFactory medioDeEnvioFactory, RepositorioNotificaciones repositorioNotificaciones) {
        this.medioDeEnvioFactory = medioDeEnvioFactory;
        this.repositorioNotificaciones = repositorioNotificaciones;
    }

    public Notificacion procesarSolicitud(SolicitudNotificacion solicitud) {
        Notificacion notificacion = crearNotificacion(solicitud);

        try {
            enviarNotificacion(
                    solicitud.getTipoMedioDeContacto(),
                    solicitud.getDireccionDeContacto(),
                    notificacion
            );
        } finally {
            repositorioNotificaciones.guardar(notificacion);
        }

        return notificacion;
    }

    private Notificacion crearNotificacion(SolicitudNotificacion solicitud) {
        String direccionDeContacto = solicitud.getDireccionDeContacto();
        String asunto = solicitud.getAsuntoMensaje();
        String cuerpo = solicitud.getCuerpoMensaje();

        Mensaje mensaje = new Mensaje(asunto, cuerpo);

        return new Notificacion(direccionDeContacto, mensaje);
    }

    public void enviarNotificacion(String tipoMedioContacto, String direccionContacto, Notificacion notificacion) {
        try {
            MedioDeEnvio medioDeContacto = medioDeEnvioFactory.mapearAMedioEnvio(tipoMedioContacto);
            medioDeContacto.enviarNotificacion(direccionContacto, notificacion);
            notificacion.marcarEnviada();
        } catch (RuntimeException ex) {
            notificacion.marcarFallida();
            throw new ErrorAlEnviarNotificacion("Ocurrio un problema inesperado al enviar la notificacion", ex);
        }
    }
}
