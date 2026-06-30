package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Obtiene el medio o los medios de contacto, hace las validaciones necesarias,
 * crea el NotificacionesDTO, y llama NotificacioneClient
 */

@Service
public class ServicioNotificaciones {

  private final NotificacionesClient notificacionesClient;

  public ServicioNotificaciones(NotificacionesClient notificacionesClient) {
    this.notificacionesClient = notificacionesClient;
  }

  public void enviarNotificacion(MedioDeContacto medioDeContacto, Mensaje mensaje) {
    validarSolicitud(medioDeContacto, mensaje);
    medioDeContacto.enviarMensaje(mensaje);

    NotificacionDTO dto = new NotificacionDTO(
        mapearTipo(medioDeContacto),
        medioDeContacto.getValor(),
        mensaje.getCuerpo(),
        mensaje.getAsunto()
    );

    notificacionesClient.enviarNotificacion(dto);
  }

  public void enviarNotificacionAMedioPredeterminado(MediosDeContacto mediosDeContacto, Mensaje mensaje) {
    if (mediosDeContacto == null || mediosDeContacto.getMedioDeContactoPredeterminado() == null) {
      throw new IllegalArgumentException("No hay un medio de contacto predeterminado para enviar la notificacion");
    }

    enviarNotificacion(mediosDeContacto.getMedioDeContactoPredeterminado(), mensaje);
  }

  public void enviarNotificacionAMediosDeContacto(MediosDeContacto mediosDeContacto, Mensaje mensaje) {
    if (mediosDeContacto == null || mediosDeContacto.getListaMediosDeContacto() == null) {
      throw new IllegalArgumentException("No hay medios de contacto para enviar la notificacion");
    }

    mediosDeContacto.getListaMediosDeContacto()
        .forEach(medioDeContacto -> enviarNotificacion(medioDeContacto, mensaje));
  }

  private void validarSolicitud(MedioDeContacto medioDeContacto, Mensaje mensaje) {
    if (medioDeContacto == null) {
      throw new IllegalArgumentException("El medio de contacto no puede ser nulo");
    }
    if (mensaje == null) {
      throw new IllegalArgumentException("El mensaje no puede ser nulo");
    }
    if (medioDeContacto.getValor() == null || medioDeContacto.getValor().isBlank()) {
      throw new IllegalArgumentException("La direccion de contacto no puede estar vacia");
    }
  }

  private String mapearTipo(MedioDeContacto medioDeContacto) {
    String tipo = medioDeContacto.getTipo();
    if (tipo == null || tipo.isBlank()) {
      throw new IllegalArgumentException("El tipo de medio de contacto no puede estar vacio");
    }

    if ("MAIL".equalsIgnoreCase(tipo) || "EMAIL".equalsIgnoreCase(tipo)) {
      return "email";
    }

    return tipo.toLowerCase(Locale.ROOT);
  }
}
