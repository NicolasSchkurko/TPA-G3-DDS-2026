package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes.NotificacionEntregaDatos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionEntregaCompletada extends EstrategiaMensaje {

    public NotificacionEntregaCompletada(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.COMPROBANTE_ENTREGA;
    }

    @Override
    public void ejecutar(Object datos) {

        NotificacionEntregaDatos evento = (NotificacionEntregaDatos) datos;

        Mensaje mensaje = new Mensaje(
                "Donación Entregada Exitosamente",
                "Comprobante de Entrega: "
                        + datosEntregaTexto(evento.getDatosEntrega()),
                TipoDeMensaje.CAMBIO_ESTADO
        );

        servicioNotificaciones.enviarNotificacionAMediosDeContacto(
                evento.getMediosDonante(),
                mensaje
        );

        servicioNotificaciones.enviarNotificacionAMediosDeContacto(
                evento.getMediosEntidad(),
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
