package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public abstract class MedioDeEnvio {
    public abstract void enviarNotificacion(String direccionEnvio, Notificacion notificacion);
}
