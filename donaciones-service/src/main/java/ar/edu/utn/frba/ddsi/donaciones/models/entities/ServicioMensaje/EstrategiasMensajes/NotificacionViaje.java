package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionViajeDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionViaje extends EstrategiaMensaje {

    public NotificacionViaje(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.DONACION_EN_VIAJE;
    }

    @Override
    public void ejecutar(Object datos) {

        NotificacionViajeDTO dto =
                (NotificacionViajeDTO) datos;

        Mensaje mensaje = new Mensaje(
                "Nueva Donación En Viaje",
                String.format(
                        "La/s donación/es se encuentra/n en viaje. Sigue la entrega: %s",
                        dto.getUrlRuta()
                ),
                TipoDeMensaje.CAMBIO_ESTADO
        );

        servicioNotificaciones.enviarNotificacionAMediosDeContacto(
                dto.getContactoDonante(),
                mensaje
        );

        servicioNotificaciones.enviarNotificacionAMediosDeContacto(
                dto.getContactoEntidad(),
                mensaje
        );
    }
}
