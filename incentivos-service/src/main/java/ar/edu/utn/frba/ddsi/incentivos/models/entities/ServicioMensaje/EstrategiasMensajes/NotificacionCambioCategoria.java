package ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.NotificacionCambioCategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionCambioCategoria extends EstrategiaMensaje {

    public NotificacionCambioCategoria(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.CAMBIO_CATEGORIA_PERSONA_DONANTE;
    }

    @Override
    public void ejecutar(Object datos) {

        NotificacionCambioCategoriaDTO dto =
                (NotificacionCambioCategoriaDTO) datos;

        Mensaje mensaje = new Mensaje(
                "Ascenso de Categoría",
                String.format(
                        "Felicitaciones %s, has ascendido de %s a %s.",
                        dto.getPerfil().getNombreUsuario(),
                        dto.getPerfilAnterior().getCategoriaActual().name(),
                        dto.getPerfil().getCategoriaActual().name()
                ),
                TipoDeMensaje.RECOMPENSAS
        );

        servicioNotificaciones.enviarNotificacion(
                dto.getMedioContacto(),
                mensaje
        );
    }
}
