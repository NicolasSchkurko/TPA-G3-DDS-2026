package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;

public class Mail extends MedioDeContacto {
    private String direccionMail;

    @Override
    public String getValor() {
        return direccionMail;
    }

    @Override
    public void enviarMensaje(String mensaje, TipoDeMensaje tipo) {
        //enviarNotificacion(tipo, "Notificación", mensaje, direccionMail);
    }
}
