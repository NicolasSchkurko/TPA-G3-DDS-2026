package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public class Whatsapp extends MedioDeEnvio{
    public Whatsapp() {}

    @Override
    public void enviarNotificacion(String direccionEnvio, Notificacion notificacion) {}
}

