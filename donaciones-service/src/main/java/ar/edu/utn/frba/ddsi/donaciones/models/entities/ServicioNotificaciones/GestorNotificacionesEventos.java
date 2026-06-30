package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Component;

/**
 * Organiza la creación de mensajes y el uso del servicio de notificaciones
 * Permite visualizar mejor los eventos en los que se hará una notificación
 */

@Component
public class GestorNotificacionesEventos {

  private final ServicioNotificaciones servicioNotificaciones;
  private final MensajesPredeterminados mensajesPredeterminados;

  public GestorNotificacionesEventos(
      ServicioNotificaciones servicioNotificaciones,
      MensajesPredeterminados mensajesPredeterminados
  ) {
    this.servicioNotificaciones = servicioNotificaciones;
    this.mensajesPredeterminados = mensajesPredeterminados;
  }

  public void notificarDonacionAsignadaAEntidadBeneficiaria(Donacion donacion) {
    notificarEventoDeDonacion(TipoEventoNotificacion.DONACION_ASIGNADA_ENTIDAD_BENEFICIARIA, donacion);
  }

  public void notificarMisionCumplidaAPersonaDonante(PersonaDonante personaDonante) {
    notificarEventoDePersonaDonante(TipoEventoNotificacion.MISION_CUMPLIDA_PERSONA_DONANTE, personaDonante);
  }

  public void notificarCambioCategoriaAPersonaDonante(PersonaDonante personaDonante) {
    notificarEventoDePersonaDonante(TipoEventoNotificacion.CAMBIO_CATEGORIA_PERSONA_DONANTE, personaDonante);
  }

  public void notificarInactividadAPersonaDonante(PersonaDonante personaDonante) {
    notificarEventoDePersonaDonante(TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE, personaDonante);
  }

  /**
   * Crea el mensaje del evento y lo envía a todos los medios de contacto de la
   * entidad beneficiaria asociada a la donacion.
   */
  private void notificarEventoDeDonacion(TipoEventoNotificacion tipoEvento, Donacion donacion) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, donacion);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
        donacion.getEntidad().getCorreosRepresentantes(),
        mensaje
    );
  }

  /**
   * Crea el mensaje del evento y lo envía al medio de contacto predeterminado
   * de la persona donante.
   */
  private void notificarEventoDePersonaDonante(TipoEventoNotificacion tipoEvento, PersonaDonante personaDonante) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, personaDonante);

    servicioNotificaciones.enviarNotificacionAMedioPredeterminado(
        personaDonante.getMediosDeContacto(),
        mensaje
    );
  }
}
