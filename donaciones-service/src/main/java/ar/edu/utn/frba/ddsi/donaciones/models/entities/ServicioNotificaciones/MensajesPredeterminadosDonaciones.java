package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Component;

/**
 * Permite crear mensajes predeterminados según el evento por el
 * que se está notificando.
 */
@Component
public class MensajesPredeterminadosDonaciones {

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, Donacion donacion) {
    return switch (tipoEvento) {
      case DONACION_ASIGNADA_ENTIDAD_BENEFICIARIA -> mensajeDonacionAsignadaAEntidadBeneficiaria(donacion);
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, String urlRuta) {
    return switch (tipoEvento) {
      case DONACION_EN_VIAJE_ENTIDAD_BENEFICIARIA -> mensajeDonacionEnViajeAEntidadBeneficiaria(urlRuta);
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, String urlRuta, EntidadBeneficiaria entidadBeneficiaria) {
    return switch (tipoEvento) {
      case DONACION_EN_VIAJE_PERSONA_DONANTE ->
          mensajeDonacionEnViajeAPersonaDonante(urlRuta, entidadBeneficiaria.getRazonSocial());
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, RutaEnProceso ruta) {
    return switch (tipoEvento) {
      case COMPROBANTE_ENTREGA_PERSONA_DONANTE, COMPROBANTE_ENTREGA_ENTIDAD_BENEFICIARIA -> mensajeComprobante(ruta);
      case ENTREGA_NO_RECIBIDA_ADMIN, ENTREGA_NO_RECIBIDA_ENTIDAD_BENEFICIARIA, ENTREGA_NO_RECIBIDA_PERSONA_DONANTE ->
          mensajeDisculpas(ruta);
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, PersonaDonante personaDonante) {
    return switch (tipoEvento) {
      case INACTIVIDAD_PERSONA_DONANTE -> mensajeInactividad(personaDonante);
      default -> throw new IllegalArgumentException("El evento no corresponde a una persona donante: " + tipoEvento);
    };
  }

  // Mensajes de donación asignada

  private Mensaje mensajeDonacionAsignadaAEntidadBeneficiaria(Donacion donacion) {
    String asunto = "Nueva donacion asignada";
    String cuerpo = String.format(
        "Se asigno una donacion a la entidad %s. Donacion: %s. Cantidad total de bienes: %d. Fecha de entrega: %s.",
        donacion.getEntidad().getRazonSocial(),
        valorOTexto(donacion.getDescripcion(), "sin descripcion"),
        donacion.sumaCantidadBienes(),
        donacion.getFechaEntrega() != null ? donacion.getFechaEntrega() : "sin fecha definida"
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  // Mensajes de donación en viaje

  private Mensaje mensajeDonacionEnViajeAEntidadBeneficiaria(String urlRuta) {
    String asunto = "Nueva Donacion En Viaje";
    String cuerpo = String.format(
        "La/s donacion/es se encuentra/n en viaje. Sigue tu entrega: %s",
        urlRuta
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  private Mensaje mensajeDonacionEnViajeAPersonaDonante(String urlRuta, String nombreEntidad) {
    String asunto = "Nueva Donacion En Viaje";
    String cuerpo = String.format(
        "Tu donacion/es se encuentra/n en viaje para la entidad %s. Sigue la entrega de tu donacion: %s",
        nombreEntidad,
        urlRuta
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  // Mensajes de entrega (comprobante / disculpas)

  private Mensaje mensajeComprobante(RutaEnProceso ruta) {
    String asunto = "Donacion Entregada Exitosamente";
    String cuerpo = "Comprobante de Entrega: " + datosDeEntrega(ruta);

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  private Mensaje mensajeDisculpas(RutaEnProceso ruta) {
    String asunto = "Entrega Fallida";
    String cuerpo = "Lo sentimos, la entrega ha fallado. " + datosDeEntrega(ruta);

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  private String datosDeEntrega(RutaEnProceso ruta) {
    return String.format(
        "Fecha entrega: %s. Hora entrega: %s. Patente camion: %s. Conductor a cargo: %s.",
        ruta.getFechaEntrega(),
        ruta.getHoraEntrega(),
        ruta.getCamionEntrega().getPatente(),
        ruta.getCamionEntrega().getNombreChofer()
    );
  }

  // Mensaje por inactividad de donante

  private Mensaje mensajeInactividad(PersonaDonante personaDonante) {
    String asunto = "Inactividad del perfil";
    String cuerpo = String.format(
        "%s, ¡te extrañamos! Hace más de 20 días que no registras actividad. Tu ayuda es muy valiosa.",
        personaDonante.darNombre()
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.ALERTA);
  }

  // Metodos privados

  private String valorOTexto(String valor, String textoPorDefecto) {
    if (valor == null || valor.isBlank()) {
      return textoPorDefecto;
    }
    return valor;
  }
}