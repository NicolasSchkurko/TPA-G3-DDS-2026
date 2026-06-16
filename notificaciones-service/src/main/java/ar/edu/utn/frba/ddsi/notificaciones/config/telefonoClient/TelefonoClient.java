package ar.edu.utn.frba.ddsi.notificaciones.config.telefonoClient;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

/**
 * Contrato para enviar notificaciones por telefono o SMS.
 * Permite usar un proveedor distinto al de mail o whatsapp.
 */
public interface TelefonoClient {
    void enviar(String direccionEnvio, Notificacion notificacion);
}
