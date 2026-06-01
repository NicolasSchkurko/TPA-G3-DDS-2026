package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public class Whatsapp extends MedioDeContacto {
    private String numero;

    public Whatsapp(String numero) {
        this.numero = numero;
    }

    @Override
    public String getValor() {
        return numero;
    }

    @Override
    public void enviarNotificacion(Notificacion notificacion) {}
}

