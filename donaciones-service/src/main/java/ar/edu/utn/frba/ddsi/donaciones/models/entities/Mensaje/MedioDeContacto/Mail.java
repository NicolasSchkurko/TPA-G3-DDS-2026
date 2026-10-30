package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mail extends MedioDeContacto {
    private String direccionMail;

    public Mail(String dirMail, List<TipoDeMensaje> tiposDeMensajeProhibidos) {
        super(tiposDeMensajeProhibidos);
        this.direccionMail = dirMail != null ? dirMail.toLowerCase().trim() : null;
    }

    public Mail(String dirMail) {
        super();
        this.direccionMail = dirMail != null ? dirMail.toLowerCase().trim() : null;
    }

    public void setDireccionMail(String dirMail) {
        this.direccionMail = dirMail != null ? dirMail.toLowerCase().trim() : null;
    }

    @Override
    public String getValor() {
        return direccionMail != null ? direccionMail.toLowerCase().trim() : null;
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
