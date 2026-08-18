package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionInactividad extends EstrategiaMensaje {

    public NotificacionInactividad(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE;
    }

    @Override
    public void ejecutar(Object datos) {

        Donante persona =
                (Donante) datos;

        Mensaje mensaje = new Mensaje(
                "Inactividad del perfil",
                String.format(
                        "%s, ¡te extrañamos! Hace más de 20 días que no registras actividad. Tu ayuda es muy valiosa.",
                        persona.darNombre()
                ),
                TipoDeMensaje.ALERTA
        );

        servicioNotificaciones.enviarNotificacionAMedioPredeterminado(
                persona.getMediosDeContacto(),
                mensaje
        );
    }
}
