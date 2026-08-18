package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionRegistroPersona extends EstrategiaMensaje {

    public NotificacionRegistroPersona(
            ServicioNotificaciones servicioNotificaciones) {
        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.REGISTRO_PERSONA;
    }

    @Override
    public void ejecutar(Object datos) {

        Donante persona = (Donante) datos;

        Mensaje mensaje = new Mensaje(
                "Nuevo Registro en DonaTrack",
                "Gracias por registrarte en DonaTrack.",
                TipoDeMensaje.BIENVENIDA
        );

        servicioNotificaciones.enviarNotificacionAMedioPredeterminado(
                persona.getMediosDeContacto(),
                mensaje
        );
    }
}
