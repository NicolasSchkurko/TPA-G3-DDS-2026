package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.config.telefonoClient.TelefonoClient;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public class Telefono extends MedioDeEnvio {
    private final TelefonoClient telefonoClient;

    public Telefono(TelefonoClient telefonoClient) {
        this.telefonoClient = telefonoClient;
    }

    @Override
    public void enviarNotificacion(String direccionEnvio, Notificacion notificacion) {
        telefonoClient.enviar(direccionEnvio, notificacion);
    }
}
