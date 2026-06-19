package ar.edu.utn.frba.ddsi.notificaciones.config.mailClient;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementacion del envio de mails mediante n8n.
 * Actualmente registra el envio; cuando se configure el workflow real, esta clase debe invocarlo.
 */
@Component("email")
public class MailClientN8N implements MailClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailClientN8N.class);

    @Override
    public void enviar(String direccionEnvio, Notificacion notificacion) {
        LOGGER.info("Solicitud de envio por mail registrada para {}", direccionEnvio);
    }
}
