package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.Camion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Time;

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


  // Mensaje cuando se le asigna una donación a una entidad beneficiaría

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


  // Mensaje por inactividad de donante

  private Mensaje mensajeInactividad(PersonaDonante personaDonante) {
    String asunto = "Inactividad del perfil";
    String cuerpo = String.format(
        "%s, ¡te extrañamos! Hace más de 20 días que no registras actividad. Tu ayuda es muy valiosa.",
        personaDonante.darNombre()
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.ALERTA);
  }


  // Mensajes de Inicio de Ruta de donación

  public Mensaje mensajeInicioRutaEntidad(Donacion donacion, String ruta) {
    String asunto = "Viene una donación en camino";
    String cuerpo = String.format(
            "Felicidades, %s, te esta por llegar una donación. Siguela aquí: %s",
            donacion.getEntidad().getRazonSocial(),
            ruta
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  public Mensaje mensajeInicioRutaDonante(Donacion donacion, String ruta) {
    String asunto = "Una de tus donaciones esta en camino";
    String cuerpo = String.format(
            "Felicidades, %s, tu donacion esta por llegar a %s. Siguela aquí: %s",
            donacion.getDonante().darNombre(),
            donacion.getEntidad().getRazonSocial(),
            ruta
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }


  // Mensaje por entrega realizada con éxito

  public Mensaje mensajeEntregaRealizadaEntidad(Donacion donacion, Date fecha, Time hora, Camion camion) {
    String asunto = "Entrega exitosa";
    String cuerpo = String.format(
            "Acabas de recibir una donación. Fecha y hora: %s %s. Camion: %s",
            fecha,
            hora,
            camion.getPatente()
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }

  public Mensaje mensajeEntregaRealizadaDonante(Donacion donacion, Date fecha, Time hora, Camion camion) {
    String asunto = "Entrega exitosa";
    String cuerpo = String.format(
            "Una de tus donaciones fue entregada. Fecha y hora: %s %s. Camion: %s",
            fecha,
            hora,
            camion.getPatente()
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }


  // Mensaje entrega no satisfactoría

  public Mensaje mensajeEntregaFallida(Donacion donacion, String motivo) {
    String asunto = "Entrega Fallida";
    String cuerpo = String.format(
            "La donación a %s no se pudo entregar por el siguiente motivo: %s",
            donacion.getEntidad().getRazonSocial(),
            motivo
    );

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.CAMBIO_ESTADO);
  }


  // Metodos privados

  private String valorOTexto(String valor, String textoPorDefecto) {
    if (valor == null || valor.isBlank()) {
      return textoPorDefecto;
    }
    return valor;
  }
}
