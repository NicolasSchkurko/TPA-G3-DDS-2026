package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionEntregaAdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionEntregaNoRecibidaAdmin extends EstrategiaMensaje {

    public NotificacionEntregaNoRecibidaAdmin(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.ENTREGA_NO_RECIBIDA_ADMIN;
    }

    @Override
    public void ejecutar(Object datos) {

        NotificacionEntregaAdminDTO dto =
                (NotificacionEntregaAdminDTO) datos;

        Mensaje mensaje = new Mensaje(
                "Entrega Fallida",
                "Lo sentimos, la entrega ha fallado.",
                TipoDeMensaje.CAMBIO_ESTADO
        );

        servicioNotificaciones.enviarNotificacionAMedioDeContacto(
                dto.getContactoAdmin(),
                mensaje
        );
    }
}
