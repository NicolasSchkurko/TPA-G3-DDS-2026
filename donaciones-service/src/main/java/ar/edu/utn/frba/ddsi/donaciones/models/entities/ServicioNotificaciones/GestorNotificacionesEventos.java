package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Component;

/**
 * Organiza la creación de mensajes y el uso del servicio de notificaciones.
 * Permite visualizar mejor los eventos en los que se hará una notificación.
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
    notificarEventoDeDonacion(TipoEventoNotificacion.DONACION_ASIGNADA_ENTIDAD_BENEFICIARIA, donacion);
  }

  public void notificarDonacionEnViajeAEntidadBeneficiaria(String urlRuta, MediosDeContacto contacto) {
    notificarEventoDeRuta(TipoEventoNotificacion.DONACION_EN_VIAJE_ENTIDAD_BENEFICIARIA, urlRuta, contacto);
  }

  public void notificarDonacionEnViajeAPersonaDonante(String urlRuta, MediosDeContacto contacto) {
    notificarEventoDeRuta(TipoEventoNotificacion.DONACION_EN_VIAJE_PERSONA_DONANTE, urlRuta, contacto);
  }

  public void notificarComprobanteEntregaAPersonaDonante(MediosDeContacto contacto, RutaEnProceso ruta) {
    notificarEventoDeRuta(TipoEventoNotificacion.COMPROBANTE_ENTREGA_PERSONA_DONANTE, ruta, contacto);
  }

  public void notificarComprobanteEntregaAEntidadBeneficiaria(MediosDeContacto contacto, RutaEnProceso ruta) {
    notificarEventoDeRuta(TipoEventoNotificacion.COMPROBANTE_ENTREGA_ENTIDAD_BENEFICIARIA, ruta, contacto);
  }

  public void notificarEntregaNoRecibidaAPersonaDonante(MediosDeContacto contacto, RutaEnProceso ruta) {
    notificarEventoDeRuta(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA_PERSONA_DONANTE, ruta, contacto);
  }

  public void notificarEntregaNoRecibidaAEntidadBeneficiaria(MediosDeContacto contacto, RutaEnProceso ruta) {
    notificarEventoDeRuta(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA_ENTIDAD_BENEFICIARIA, ruta, contacto);
  }

  public void notificarEntregaNoRecibidaAdmin(MedioDeContacto contacto, RutaEnProceso ruta) {
    notificarEventoDeRutaAdmin(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA_ADMIN, ruta, contacto);
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

  private void notificarEventoDeRuta(TipoEventoNotificacion tipoEvento, String urlRuta, MediosDeContacto contacto) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, urlRuta);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(contacto, mensaje);
  }

  private void notificarEventoDeRutaAdmin(TipoEventoNotificacion tipoEvento, RutaEnProceso ruta, MedioDeContacto contacto) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, ruta);

    servicioNotificaciones.enviarNotificacionAMedioDeContacto(
            contacto,
            mensaje
    );
  }

  private void notificarEventoDeRuta(TipoEventoNotificacion tipoEvento, RutaEnProceso ruta, MediosDeContacto contacto) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, ruta);

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(contacto, mensaje);
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