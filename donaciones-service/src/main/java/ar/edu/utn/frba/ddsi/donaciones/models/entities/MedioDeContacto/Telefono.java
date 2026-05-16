package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Telefono extends MedioDeContacto {
    private String numeroDeTelefono;

    public Telefono(String numTelefono){
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
