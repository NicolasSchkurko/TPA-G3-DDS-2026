package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.ErrorAlEnviarNotificacion;
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

  public GestorNotificaciones(RepositorioNotificaciones repositorioNotificaciones, MedioDeEnvioFactory factory) {
    this.repositorioNotificaciones = repositorioNotificaciones;
    this.factory = factory; //No se donde se asigna
  }

  public void enviarSolicitudDeNotificacion(String tipoDeMedioDeContacto, String direccionDeContacto, String asunto, String cuerpo){

        Notificacion notificacion = crearNotificacion(direccionDeContacto, asunto, cuerpo);
        enviarNotificacion(tipoDeMedioDeContacto,direccionDeContacto, notificacion);

    }

    // Crea una Notificacion a partir de una SolicitudNotificacion y la guarda en el repositorio
    public Notificacion crearNotificacion(String direccionDeContacto, String asunto, String cuerpo) {

        Mensaje mensaje = new Mensaje(asunto, cuerpo);
        Notificacion notificacion = new Notificacion(direccionDeContacto, mensaje);

        repositorioNotificaciones.guardar(notificacion);

        return new Notificacion(direccionDeContacto, mensaje);
    }

    // Por ahora solo envia al medio predeterminado
    public void enviarNotificacion(String tipoMedioContacto ,String direccionContacto, Notificacion notificacion) {

        try {
            MedioDeEnvio medioDeContacto = factory.mapearAMedioEnvio(tipoMedioContacto);
            medioDeContacto.enviarNotificacion(notificacion);
            notificacion.marcarEnviada();

        } catch (IllegalArgumentException ex) {

            notificacion.marcarFallida();
            throw new ErrorAlEnviarNotificacion("Ocurrio un problema inesperado al enviar la notificacion", ex);
        }
    }

    public Optional<Notificacion> obtenerNotificacionPorId(UUID id) {
        return repositorioNotificaciones.findById(id);
    }
}
