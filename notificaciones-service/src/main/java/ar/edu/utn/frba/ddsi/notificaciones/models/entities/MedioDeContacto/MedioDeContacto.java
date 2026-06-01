package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.TipoNotificacion;

import java.util.List;

public abstract class MedioDeContacto {
    private List<TipoNotificacion> tiposNotificacionAdmitidos;

    public MedioDeContacto(List<TipoNotificacion> tiposNotificacionAdmitidos){
        this.tiposNotificacionAdmitidos = tiposNotificacionAdmitidos;
    }

    public MedioDeContacto() {
        this.tiposNotificacionAdmitidos = List.of(
                TipoNotificacion.BIENVENIDA,
                TipoNotificacion.ALERTA,
                TipoNotificacion.CAMBIO_ESTADO_DONACION,
                TipoNotificacion.MISION,
                TipoNotificacion.RECOMPENSA_DESBLOQUEADA,
                TipoNotificacion.ENTREGA_CONFIRMADA,
                TipoNotificacion.ASIGNACION_DONACION,
                TipoNotificacion.CAMBIO_CATEGORIA
        );
    }

    public abstract String getValor();

    public void enviarNotificacion(Notificacion notificacion) {
        if (!tiposNotificacionAdmitidos.contains(notificacion.getTipo())) {
            throw new IllegalArgumentException("Tipo de mensaje no permitido: " + notificacion.getTipo());
        }
    }

    public void agregarTipoDeMensajeAdmitido(TipoNotificacion tipoNotificacion){
        this.tiposNotificacionAdmitidos.add(tipoNotificacion);
    }

    public void eliminarTipoDeMensajeAdmitido(TipoNotificacion tipoNotificacion){
        this.tiposNotificacionAdmitidos.remove(tipoNotificacion);
    }
}
