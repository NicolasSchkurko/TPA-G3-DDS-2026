package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter

public class Mail extends MedioDeContacto {
    private String direccionMail;

    public Mail(String dirMail, List<TipoDeMensaje> tiposDeMensajeProhibidos) {
        super(tiposDeMensajeProhibidos);
        this.direccionMail = dirMail;
    }

    public Mail(String dirMail) {
        super();
        this.direccionMail = dirMail;
    }

    @Override
    public String getValor() {
        return direccionMail;
    }

    @Override
    public String getTipo() {
        return "EMAIL";
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
