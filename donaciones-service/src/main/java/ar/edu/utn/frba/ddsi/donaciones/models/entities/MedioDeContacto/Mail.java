package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Mail extends MedioDeContacto {

    private String direccionMail;

    public Mail(String dirMail, List<TipoDeMensaje> tiposDeMensajeAdmitidos) {
        super(tiposDeMensajeAdmitidos);
        this.direccionMail=dirMail;
    }

    public Mail(String dirMail){
        super();
        this.direccionMail=dirMail;
    }

    @Override
    public String getValor() {
        return direccionMail;
    }

    @Override
    public void enviarMensaje(Mensaje mensaje) {
        super.enviarMensaje(mensaje);
        //enviarNotificacion(tipo, "Notificación", mensaje.getTexto(), direccionMail);
    }

    @Override
    public String toString() {
        return "Mail{direccionMail=" + direccionMail + '}';
    }
}
