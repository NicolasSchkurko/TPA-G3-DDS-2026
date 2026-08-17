package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionEntregaFallidaAdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionEntregaFallidaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;

public class NotificacionEntregaFallida extends EstrategiaMensaje {
    public NotificacionEntregaFallida(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.ENTREGA_NO_RECIBIDA;
    }

    @Override
    public void ejecutar(Object datos) {

        NotificacionEntregaFallidaDTO dto =
                (NotificacionEntregaFallidaDTO) datos;

        Mensaje mensaje = new Mensaje(
                "Entrega Fallida",
                "Lo sentimos, la entrega ha fallado.",
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

        dto.getContactosAdmin().forEach(
                contacto ->
                        servicioNotificaciones.enviarNotificacionAMedioDeContacto(
                            contacto,
                            mensaje
                ));
    }
}
