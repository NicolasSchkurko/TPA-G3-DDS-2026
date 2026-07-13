package ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.NotificacionMisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionMisionCumplida extends EstrategiaMensaje {

    public NotificacionMisionCumplida(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.MISION_CUMPLIDA_PERSONA_DONANTE;
    }

    @Override
    public void ejecutar(Object datos) {

        NotificacionMisionDTO dto =
                (NotificacionMisionDTO) datos;

        Perfil perfil = dto.getPerfil();

        Mensaje mensaje = new Mensaje(
                "Misión Completa",
                String.format(
                        """
                        Felicitaciones %s.

                        Has conseguido una nueva insignia:

                        %s

                        %s

                        %s
                        """,
                        perfil.getNombreUsuario(),
                        perfil.getInsignias().getLast().getNombre(),
                        perfil.getInsignias().getLast().getDescripcion(),
                        perfil.getInsignias().getLast().getUrlImagen()
                ),
                TipoDeMensaje.MISION
        );

        servicioNotificaciones.enviarNotificacion(
                dto.getMedioContacto(),
                mensaje
        );
    }
}