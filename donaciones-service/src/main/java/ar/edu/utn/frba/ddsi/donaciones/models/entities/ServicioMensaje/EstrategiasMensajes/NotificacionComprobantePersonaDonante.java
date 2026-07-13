package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionComprobantePersonaDonante extends EstrategiaMensaje {

    public NotificacionComprobantePersonaDonante(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.COMPROBANTE_ENTREGA_PERSONA_DONANTE;
    }

    @Override
    public void ejecutar(Object datos) {

        NotificacionEntregaDTO dto =
                (NotificacionEntregaDTO) datos;

        Mensaje mensaje = new Mensaje(
                "Donación Entregada Exitosamente",
                "Comprobante de Entrega: "
                        + datosEntregaTexto(dto.getDatosEntrega()),
                TipoDeMensaje.CAMBIO_ESTADO
        );

        servicioNotificaciones.enviarNotificacionAMediosDeContacto(
                dto.getDestinatarios(),
                mensaje
        );
    }

    private String datosEntregaTexto(
            PayloadEntregaDTO datos) {

        return String.format(
                "Fecha entrega: %s. Hora entrega: %s. Patente camión: %s. Conductor a cargo: %s.",
                valorOTexto(datos.getFechaEntrega(), "sin dato"),
                valorOTexto(datos.getHoraEntrega(), "sin dato"),
                valorOTexto(datos.getPatenteCamion(), "sin dato"),
                valorOTexto(datos.getNombreChofer(), "sin dato")
        );
    }
}
