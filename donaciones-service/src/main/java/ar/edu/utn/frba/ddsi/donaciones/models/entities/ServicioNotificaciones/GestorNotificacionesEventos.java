package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
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
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(
        TipoEventoNotificacion.DONACION_ASIGNADA_ENTIDAD_BENEFICIARIA, donacion);
    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
        donacion.getEntidad().getCorreosRepresentantes(), mensaje);
  }

  public void notificarDonacionEnViajeAEntidadBeneficiaria(String urlRuta, MediosDeContacto contacto) {
    notificarEventoDeViaje(TipoEventoNotificacion.DONACION_EN_VIAJE_ENTIDAD_BENEFICIARIA, urlRuta, contacto);
  }

  public void notificarDonacionEnViajeAPersonaDonante(String urlRuta, MediosDeContacto contacto) {
    notificarEventoDeViaje(TipoEventoNotificacion.DONACION_EN_VIAJE_PERSONA_DONANTE, urlRuta, contacto);
  }

  public void notificarComprobanteEntregaAPersonaDonante(MediosDeContacto contacto, PayloadEntregaDTO datos) {
    notificarEventoDeEntrega(TipoEventoNotificacion.COMPROBANTE_ENTREGA_PERSONA_DONANTE, datos, contacto);
  }

  public void notificarComprobanteEntregaAEntidadBeneficiaria(MediosDeContacto contacto, PayloadEntregaDTO datos) {
    notificarEventoDeEntrega(TipoEventoNotificacion.COMPROBANTE_ENTREGA_ENTIDAD_BENEFICIARIA, datos, contacto);
  }

  public void notificarEntregaNoRecibidaAPersonaDonante(MediosDeContacto contacto, PayloadEntregaDTO datos) {
    notificarEventoDeEntrega(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA_PERSONA_DONANTE, datos, contacto);
  }

  public void notificarEntregaNoRecibidaAEntidadBeneficiaria(MediosDeContacto contacto, PayloadEntregaDTO datos) {
    notificarEventoDeEntrega(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA_ENTIDAD_BENEFICIARIA, datos, contacto);
  }

  public void notificarEntregaNoRecibidaAdmin(MedioDeContacto contactoAdmin, PayloadEntregaDTO datos) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA_ADMIN, datos);
    servicioNotificaciones.enviarNotificacionAMedioDeContacto(contactoAdmin, mensaje);
  }

  public void notificarInactividadAPersonaDonante(PersonaDonante personaDonante) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(
        TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE, personaDonante);
    servicioNotificaciones.enviarNotificacionAMedioPredeterminado(
        personaDonante.getMediosDeContacto(), mensaje);
  }

  private void notificarEventoDeViaje(TipoEventoNotificacion tipoEvento, String urlRuta, MediosDeContacto contacto) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, urlRuta);
    servicioNotificaciones.enviarNotificacionAMediosDeContacto(contacto, mensaje);
  }

  private void notificarEventoDeEntrega(TipoEventoNotificacion tipoEvento, PayloadEntregaDTO datos, MediosDeContacto contacto) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(tipoEvento, datos);
    servicioNotificaciones.enviarNotificacionAMediosDeContacto(contacto, mensaje);
  }
}