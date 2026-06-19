package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.config.mailClient.MailClient;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public class Mail extends MedioDeEnvio {
    private final MailClient mailClient;

    public Mail(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    @Override
    public void enviarNotificacion(String direccionEnvio, Notificacion notificacion) {
        mailClient.enviar(direccionEnvio, notificacion);
    }
}
