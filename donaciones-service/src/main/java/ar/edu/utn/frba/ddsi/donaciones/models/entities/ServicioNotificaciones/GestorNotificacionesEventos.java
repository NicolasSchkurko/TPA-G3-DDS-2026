package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
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
  private final MensajesPredeterminadosDonaciones mensajesPredeterminados;

  public GestorNotificacionesEventos(
      ServicioNotificaciones servicioNotificaciones,
      MensajesPredeterminadosDonaciones mensajesPredeterminados
  ) {
    this.servicioNotificaciones = servicioNotificaciones;
    this.mensajesPredeterminados = mensajesPredeterminados;
  }

  public void notificarDonacionAsignadaAEntidadBeneficiaria(Donacion donacion) {
    notificarEventoAEntidad(TipoEventoNotificacion.DONACION_ASIGNADA_ENTIDAD_BENEFICIARIA, donacion);
  }

  public void notificarInactividadAPersonaDonante(PersonaDonante personaDonante) {
    notificarEventoAPersonaDonante(TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE, personaDonante);
  }

  /**
   * Crea el mensaje del evento y lo envía a todos los medios de contacto de la
   * entidad beneficiaria asociada a la donacion.
   */
  public void notificarEventoAEntidad(TipoEventoNotificacion tipoEvento, Donacion donacion) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, donacion);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
        donacion.getEntidad().getCorreosRepresentantes(),
        mensaje
    );
  }

  /**
   * Crea el mensaje del evento y lo envía a todos los medios de contacto de la
   * entidad beneficiaria.
   */
  public void notificarEventoAEntidad(TipoEventoNotificacion tipoEvento, EntidadBeneficiaria entidadBeneficiaria) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, entidadBeneficiaria);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
            entidadBeneficiaria.getCorreosRepresentantes(),
            mensaje
    );
  }

  /**
   * Crea el mensaje del evento y lo envía a todos los medios de contacto de la
   * persona donante asociada a la donacion.
   */
  public void notificarEventoAPersonaDonante(TipoEventoNotificacion tipoEvento, Donacion donacion) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, donacion);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
            donacion.getDonante().getMediosDeContacto(),
            mensaje
    );
  }

  /**
   * Crea el mensaje del evento y lo envía al medio de contacto predeterminado
   * de la persona donante.
   */
  public void notificarEventoAPersonaDonante(TipoEventoNotificacion tipoEvento, PersonaDonante personaDonante) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, personaDonante);

    servicioNotificaciones.enviarNotificacionAMedioPredeterminado(
        personaDonante.getMediosDeContacto(),
        mensaje
    );
  }
}
