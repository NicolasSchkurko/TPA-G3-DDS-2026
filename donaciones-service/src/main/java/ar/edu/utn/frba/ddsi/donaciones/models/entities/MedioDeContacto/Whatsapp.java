package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class Whatsapp extends MedioDeContacto {


    private String numeroDeTelefono;

    public Whatsapp(String numTelefono){
        this.numeroDeTelefono=numTelefono;
    }

    @Override
    public String getValor() {
        return numeroDeTelefono;
    }

    @Override
    public void enviarMensaje(String mensaje, TipoDeMensaje tipo) {
        //enviarNotificacion(tipo, "Notificación", mensaje, numeroDeTelefono);
    }
}
