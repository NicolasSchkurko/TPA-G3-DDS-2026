package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.Camion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Time;

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

  public void notificarInicioRuta(Donacion donacion, String ruta) {
    Mensaje mensajeDonante = mensajesPredeterminados.mensajeInicioRutaDonante(donacion, ruta);
    Mensaje mensajeEntidad = mensajesPredeterminados.mensajeInicioRutaEntidad(donacion, ruta);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
            donacion.getDonante().getMediosDeContacto(),
            mensajeDonante
    );

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
            donacion.getEntidad().getCorreosRepresentantes(),
            mensajeEntidad
    );
  }

  public void notificarEntregaExitosa(Donacion donacion, Date fecha, Time hora, Camion camion) {
    Mensaje mensajeDonante = mensajesPredeterminados.mensajeEntregaRealizadaDonante(donacion, fecha, hora, camion);
    Mensaje mensajeEntidad = mensajesPredeterminados.mensajeEntregaRealizadaEntidad(donacion, fecha, hora, camion);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
            donacion.getDonante().getMediosDeContacto(),
            mensajeDonante
    );

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
            donacion.getEntidad().getCorreosRepresentantes(),
            mensajeEntidad
    );
  }

  public void notificarEntregaFallida(Donacion donacion, String motivo) {
    Mensaje mensaje = mensajesPredeterminados.mensajeEntregaFallida(donacion, motivo);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
            donacion.getDonante().getMediosDeContacto(),
            mensaje
    );

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
            donacion.getEntidad().getCorreosRepresentantes(),
            mensaje
    );
  }

  /**
   * Crea el mensaje del evento y lo envía a todos los medios de contacto de la
   * entidad beneficiaria asociada a la donacion.
   */
  private void notificarEventoAEntidad(TipoEventoNotificacion tipoEvento, Donacion donacion) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, donacion);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
        donacion.getEntidad().getCorreosRepresentantes(),
        mensaje
    );
  }


  // Metodos opcionales

  /**
   * Crea el mensaje del evento y lo envía a todos los medios de contacto de la
   * entidad beneficiaria.
   */
  private void notificarEventoAEntidad(TipoEventoNotificacion tipoEvento, EntidadBeneficiaria entidadBeneficiaria) {
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
  private void notificarEventoAPersonaDonante(TipoEventoNotificacion tipoEvento, Donacion donacion) {
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
  private void notificarEventoAPersonaDonante(TipoEventoNotificacion tipoEvento, PersonaDonante personaDonante) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, personaDonante);

    servicioNotificaciones.enviarNotificacionAMedioPredeterminado(
        personaDonante.getMediosDeContacto(),
        mensaje
    );
  }
}
