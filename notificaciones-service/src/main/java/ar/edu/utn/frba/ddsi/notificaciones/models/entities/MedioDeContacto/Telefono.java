package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import com.fasterxml.jackson.databind.util.LinkedNode;

public class Telefono extends MedioDeContacto {
    private String numero;

    public Telefono(String numero) {
        this.numero = numero;
    }

    @Override
    public String getValor() {
        return numero;
    }

    @Override
    public void enviarNotificacion(Notificacion notificacion) {}
}

