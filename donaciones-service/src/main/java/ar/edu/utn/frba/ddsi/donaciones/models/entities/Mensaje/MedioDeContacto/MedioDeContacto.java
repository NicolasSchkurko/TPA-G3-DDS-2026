package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter

public abstract class MedioDeContacto {
    //private List<TipoDeMensaje> tiposDeMensajeProhibidos;

    public MedioDeContacto(List<TipoDeMensaje> tiposDeMensajeProhibidos) {
       // this.tiposDeMensajeProhibidos = new ArrayList<>(tiposDeMensajeProhibidos);
    }

    public MedioDeContacto() {
      //  this.tiposDeMensajeProhibidos = new ArrayList<>();
    }

    public abstract String getValor();

    public abstract String getTipo();

    public void enviarMensaje(Mensaje mensaje) {
//        if (tiposDeMensajeProhibidos.contains(mensaje.getTipoDeMensaje())) {
//            throw new IllegalArgumentException("Tipo de mensaje no permitido: " + mensaje.getTipoDeMensaje());
//        }
    }

//    public void agregarTipoDeMensajeProhibido(TipoDeMensaje tipoDeMensaje) {
//        this.tiposDeMensajeProhibidos.add(tipoDeMensaje);
//    }

//    public void eliminarTipoDeMensajeProhibido(TipoDeMensaje tipoDeMensaje) {
//        this.tiposDeMensajeProhibidos.remove(tipoDeMensaje);
//    }


}
