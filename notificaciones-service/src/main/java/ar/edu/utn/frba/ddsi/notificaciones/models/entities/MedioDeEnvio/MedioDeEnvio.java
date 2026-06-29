package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.gateways.NotificacionGateway;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public abstract class MedioDeEnvio {

    protected NotificacionGateway gateway;

    public MedioDeEnvio(NotificacionGateway gateway) {
        this.gateway = gateway;
    }



    public abstract void enviarNotificacion(Notificacion notificacion);
}
