package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

// Esta clase no será más que un solo medio de envío de notificaciones
public abstract class MedioDeEnvio {
    public MedioDeEnvio() {}

    // Cada medio de envío se encargará de implementar su propia forma de enviar la notificacion
    public void enviarNotificacion(String direccionEnvio, Notificacion notificacion) {}
}
