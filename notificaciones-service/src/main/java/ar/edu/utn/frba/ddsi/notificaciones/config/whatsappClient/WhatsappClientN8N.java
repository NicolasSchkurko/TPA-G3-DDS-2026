package ar.edu.utn.frba.ddsi.notificaciones.config.whatsappClient;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementacion del envio por whatsapp mediante n8n.
 * Actualmente registra el envio; cuando se configure el workflow real, esta clase debe invocarlo.
 */
@Component
public class WhatsappClientN8N implements WhatsappClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsappClientN8N.class);

    @Override
    public void enviar(String direccionEnvio, Notificacion notificacion) {
        LOGGER.info("Solicitud de envio por whatsapp registrada para {}", direccionEnvio);
    }
}
