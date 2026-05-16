package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Mail extends MedioDeContacto {

    private String direccionMail;

    public Mail(String dirMail) {
        this.direccionMail=dirMail;
    }

    @Override
    public String getValor() {
        return direccionMail;
    }

    @Override
    public void enviarMensaje(String mensaje, TipoDeMensaje tipo) {
        //enviarNotificacion(tipo, "Notificación", mensaje, direccionMail);
    }

}
