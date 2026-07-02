package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Component;

/**
 * Permite crear mensajes predeterminados según el evento por el
 * que se está notificando
 */

@Component
public class MensajesPredeterminadosDonaciones {

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, Donacion donacion) {
    return switch (tipoEvento) {
      case DONACION_ASIGNADA_ENTIDAD_BENEFICIARIA -> mensajeDonacionAsignadaAEntidadBeneficiaria(donacion);
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, String ruta) {
    return switch (tipoEvento) {
      case DONACION_EN_VIAJE_ENTIDAD_BENEFICIARIA -> mensajeDonacionEnViajeAEntidadBeneficiaria(ruta);
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, RutaEnProceso ruta) {
    return switch (tipoEvento) {
      case COMPROBANTE_ENTREGA_PERSONA_DONANTE -> mensajeComprobante(ruta);
      case COMPROBANTE_ENTREGA_ENTIDAD_BENEFICIARIA -> mensajeComprobante(ruta);
      case ENTREGA_NO_RECIBIDA_ADMIN -> mensajeDisculpas(ruta);
      case ENTREGA_NO_RECIBIDA_ENTIDAD_BENEFICIARIA -> mensajeDisculpas(ruta);
      case ENTREGA_NO_RECIBIDA_PERSONA_DONANTE -> mensajeDisculpas(ruta);
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  public Mensaje mensajeComprobante(RutaEnProceso ruta) {
    String asunto = "Donacion Entregada Exitosamente";
    String cuerpo = String.format(
            "Comprobante de Entrega:" +
                    "fecha entrega: %s," +
                    "hora entrega: %s." +
                    "patente camion: %s," +
                    "conductor a cargo: %s",
            ruta.getFechaEntrega(),
            ruta.getHoraEntrega(),
            ruta.getCamionEntrega().getPatente(),
            ruta.getCamionEntrega().getNombreChofer()

    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  public Mensaje mensajeDisculpas(RutaEnProceso ruta) {
    String asunto = "Entrega Fallida";
    String cuerpo = String.format(
            "lo sentimos, la entrega:" +
                    "fecha entrega: %s," +
                    "hora entrega: %s." +
                    "patente camion: %s," +
                    "conductor a cargo: %s." +
                    "ha fallado",
            ruta.getFechaEntrega(),
            ruta.getHoraEntrega(),
            ruta.getCamionEntrega().getPatente(),
            ruta.getCamionEntrega().getNombreChofer()

    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, String ruta, String nomEntidad) {
    return switch (tipoEvento) {
      case DONACION_EN_VIAJE_PERSONA_DONANTE -> mensajeDonacionEnViajeAPersonaDonante(ruta, nomEntidad);
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  private Mensaje mensajeDonacionEnViajeAEntidadBeneficiaria(String url) {
    String asunto = "Nueva Donacion En Viaje";
    String cuerpo = String.format(
            "la/s donacion/es" +
                    "se encuentra en viaje. Sigue tu entrega: %s",
            url
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  private Mensaje mensajeDonacionEnViajeAPersonaDonante(String url, String nomEntidad) {
    String asunto = "Nueva Donacion En Viaje";
    String cuerpo = String.format(
            "tu donacion/es se encuentra en viaje para la entidad %s." +
                    "Sigue la entrega de tu donacion: %s",
            nomEntidad,
            url
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, PersonaDonante personaDonante) {
    return switch (tipoEvento) {
      case INACTIVIDAD_PERSONA_DONANTE -> mensajeInactividad(personaDonante);
      default -> throw new IllegalArgumentException("El evento no corresponde a una persona donante: " + tipoEvento);
    };
  }

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

  private Mensaje mensajeInactividad(PersonaDonante personaDonante) {
    String asunto = "Inactividad del perfil";
    String cuerpo = String.format(
        "%s, ¡te extrañamos! Hace más de 20 días que no registras actividad. Tu ayuda es muy valiosa.",
        personaDonante.darNombre()
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.ALERTA);
  }

  private String valorOTexto(String valor, String textoPorDefecto) {
    if (valor == null || valor.isBlank()) {
      return textoPorDefecto;
    }
    return valor;
  }
}
