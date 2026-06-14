package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public class Mail extends MedioDeEnvio {
    public Mail() {}

    @Override
    public void enviarNotificacion(String direccionEnvio, Notificacion notificacion) {}
}
