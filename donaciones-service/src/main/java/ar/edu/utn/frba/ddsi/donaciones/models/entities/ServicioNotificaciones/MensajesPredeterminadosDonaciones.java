package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
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
      case INICIO_RUTA_DONANTE -> mensajeInicioRutaDonante(donacion);
      case INICIO_RUTA_ENTIDAD_BENEFICIARIA -> mensajeInicioRutaEntidad(donacion);
      default -> throw new IllegalArgumentException("El evento no corresponde a una donacion: " + tipoEvento);
    };
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, PersonaDonante personaDonante) {
    return switch (tipoEvento) {
      case INACTIVIDAD_PERSONA_DONANTE -> mensajeInactividad(personaDonante);
      default -> throw new IllegalArgumentException("El evento no corresponde a una persona donante: " + tipoEvento);
    };
  }

  public Mensaje crearMensaje(TipoEventoNotificacion tipoEvento, EntidadBeneficiaria entidadBeneficiaria) {
//    return switch (tipoEvento) {
//      default -> throw new IllegalArgumentException("El evento no corresponde a una persona donante: " + tipoEvento);
//    };
    return null;
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

  private Mensaje mensajeInicioRutaEntidad(Donacion donacion) {
    String asunto = "Viene una donación en camino";
    String cuerpo = String.format(
            "Felicidades, %s, te esta por llegar una donación",
            donacion.getEntidad().getRazonSocial()
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  private Mensaje mensajeInicioRutaDonante(Donacion donacion) {
    String asunto = "Una de tus donaciones esta en camino";
    String cuerpo = String.format(
            "Felicidades, %s, tu donacion esta por llegar a %s",
            donacion.getDonante().darNombre(),
            donacion.getEntidad().getRazonSocial()
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  private String valorOTexto(String valor, String textoPorDefecto) {
    if (valor == null || valor.isBlank()) {
      return textoPorDefecto;
    }
    return valor;
  }
}
