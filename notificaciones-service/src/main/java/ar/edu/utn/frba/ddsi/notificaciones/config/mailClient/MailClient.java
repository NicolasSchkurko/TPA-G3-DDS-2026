package ar.edu.utn.frba.ddsi.notificaciones.config.mailClient;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

/**
 * Contrato para enviar notificaciones por mail.
 * Permite cambiar el proveedor concreto sin modificar el medio Mail.
 */
public interface MailClient {
    void enviar(String direccionEnvio, Notificacion notificacion);
}
