package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public class Mail extends MedioDeContacto {
    private String direccion;

    public Mail(String direccion) {
        this.direccion = direccion;
    }

    @Override
    public String getValor() {
        return direccion;
    }

    @Override
    public void enviarNotificacion(Notificacion notificacion) {}
}
