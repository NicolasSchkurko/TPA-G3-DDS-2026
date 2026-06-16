package ar.edu.utn.frba.ddsi.notificaciones.config.whatsappClient;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

/**
 * Contrato para enviar notificaciones por whatsapp.
 * Permite cambiar la integracion concreta sin modificar el medio Whatsapp.
 */
public interface WhatsappClient {
    void enviar(String direccionEnvio, Notificacion notificacion);
}
