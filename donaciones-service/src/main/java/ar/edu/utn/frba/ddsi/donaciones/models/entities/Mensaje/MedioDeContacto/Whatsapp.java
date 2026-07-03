package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter

public class Whatsapp extends MedioDeContacto {


    private String numeroDeTelefono;

    public Whatsapp(String numTelefono, List<TipoDeMensaje> tiposDeMensajeProhibidos) {
        super(tiposDeMensajeProhibidos);
        this.numeroDeTelefono = numTelefono;
    }

    public Whatsapp(String numTelefono) {
        super();
        this.numeroDeTelefono = numTelefono;
    }

    @Override
    public String getValor() {
        return numeroDeTelefono;
    }

    @Override
    public String getTipo() {
        return "WHATSAPP";
    }

    @Override
    public void enviarMensaje(Mensaje mensaje) {
        super.enviarMensaje(mensaje);
        //enviarNotificacion(tipo, "Notificación", mensaje.getTexto(), numeroDeTelefono);
    }
}
