package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class Telefono extends MedioDeContacto {
    private String numeroDeTelefono;

    public Telefono(String numTelefono, List<TipoDeMensaje> tiposDeMensajeProhibidos) {
        super(tiposDeMensajeProhibidos);
        this.numeroDeTelefono = numTelefono;
    }

    public Telefono(String numTelefono) {
        super();
        this.numeroDeTelefono = numTelefono;
    }

    @Override
    public String getValor() {
        return numeroDeTelefono;
    }

    @Override
    public String getTipo() {
        return "TELEFONO";
    }

    @Override
    public void enviarMensaje(Mensaje mensaje) {
        super.enviarMensaje(mensaje);
        //enviarNotificacion(tipo, "Notificación", mensaje.getTexto(), numeroDeTelefono);
    }

    @Override
    public String toString() {
        return "Telefono{numeroDeTelefono=" + numeroDeTelefono + '}';
    }
}
