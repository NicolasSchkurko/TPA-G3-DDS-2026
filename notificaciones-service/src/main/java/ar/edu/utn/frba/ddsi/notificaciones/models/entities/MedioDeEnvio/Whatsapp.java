package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.config.whatsappClient.WhatsappClient;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public class Whatsapp extends MedioDeEnvio {
    private final WhatsappClient whatsappClient;

    public Whatsapp(WhatsappClient whatsappClient) {
        this.whatsappClient = whatsappClient;
    }

    @Override
    public void enviarNotificacion(String direccionEnvio, Notificacion notificacion) {
        whatsappClient.enviar(direccionEnvio, notificacion);
    }
}
