package ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.incentivos.dto.MedioContactoDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.springframework.stereotype.Component;

/**
 * Organiza la creación de mensajes y el uso del servicio de notificaciones
 * Permite visualizar mejor los eventos en los que se hará una notificación
 */

@Component
public class GestorNotificacionesEventos {

  private final ServicioNotificaciones servicioNotificaciones;
  private final MensajesPredeterminadosIncentivos mensajesPredeterminados;

  public GestorNotificacionesEventos(
      ServicioNotificaciones servicioNotificaciones,
      MensajesPredeterminadosIncentivos mensajesPredeterminados
  ) {
    this.servicioNotificaciones = servicioNotificaciones;
    this.mensajesPredeterminados = mensajesPredeterminados;
  }

  public void notificarMisionCumplidaAPerfil(Perfil perfil, MedioContactoDTO medioDeContacto) {
    Mensaje mensaje = mensajesPredeterminados.mensajeMisionCumplida(perfil);

    servicioNotificaciones.enviarNotificacion(
            medioDeContacto,
            mensaje
    );
  }

  public void notificarCambioCategoriaAPerfil(Perfil perfil, Perfil perfilAnterior, MedioContactoDTO medioDeContacto) {
    Mensaje mensaje = mensajesPredeterminados.mensajeCambioCategoria(perfil, perfilAnterior);

    servicioNotificaciones.enviarNotificacion(
            medioDeContacto,
            mensaje
    );
  }
}
