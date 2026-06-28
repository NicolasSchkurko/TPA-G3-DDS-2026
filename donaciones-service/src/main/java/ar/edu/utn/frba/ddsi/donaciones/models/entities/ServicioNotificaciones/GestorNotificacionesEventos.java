package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Component;

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
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(
        TipoEventoNotificacion.DONACION_ASIGNADA_ENTIDAD_BENEFICIARIA,
        donacion
    );

    servicioNotificaciones.enviarNotificacionAMediosDeContacto(
        donacion.getEntidad().getCorreosRepresentantes(),
        mensaje
    );
  }

  public void notificarMisionCumplidaAPersonaDonante(PersonaDonante personaDonante) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(
        TipoEventoNotificacion.MISION_CUMPLIDA_PERSONA_DONANTE,
        personaDonante
    );

    servicioNotificaciones.enviarNotificacionAMedioPredeterminado(
        personaDonante.getMediosDeContacto(),
        mensaje
    );
  }

  public void notificarCambioCategoriaAPersonaDonante(PersonaDonante personaDonante) {
    Mensaje mensaje = mensajesPredeterminados.crearMensaje(
        TipoEventoNotificacion.CAMBIO_CATEGORIA_PERSONA_DONANTE,
        personaDonante
    );

    servicioNotificaciones.enviarNotificacionAMedioPredeterminado(
        personaDonante.getMediosDeContacto(),
        mensaje
    );
  }
}
