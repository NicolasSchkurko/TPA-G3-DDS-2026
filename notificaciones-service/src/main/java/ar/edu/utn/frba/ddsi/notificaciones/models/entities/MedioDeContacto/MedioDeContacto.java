package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.TipoNotificacion;

import java.util.ArrayList;
import java.util.List;

public abstract class MedioDeContacto {
    private List<TipoNotificacion> tiposNotificacionProhibidos = new ArrayList<>();

    public MedioDeContacto(List<TipoNotificacion> tiposNotificacionProhibidos){
        this.tiposNotificacionProhibidos = tiposNotificacionProhibidos;
    }

    public MedioDeContacto() {}

    public abstract String getValor();

    public void enviarNotificacion(Notificacion notificacion) {
        if (tiposNotificacionProhibidos.contains(notificacion.getTipo())) {
            throw new IllegalArgumentException("Tipo de mensaje no permitido: " + notificacion.getTipo());
        }
    }

    public void agregarTipoNotificacionProhibido(TipoNotificacion tipoNotificacion){
        this.tiposNotificacionProhibidos.add(tipoNotificacion);
    }

    public void eliminarTipoNotificacionProhibido(TipoNotificacion tipoNotificacion){
        this.tiposNotificacionProhibidos.remove(tipoNotificacion);
    }

    public Boolean permiteTipoNotificacion(TipoNotificacion tipoNotificacion){
        return !this.tiposNotificacionProhibidos.contains(tipoNotificacion);
    }
}
