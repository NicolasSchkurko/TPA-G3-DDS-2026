package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public abstract class MedioDeContacto {
    private List<TipoDeMensaje> tiposDeMensajeAdmitidos;

    public MedioDeContacto(List<TipoDeMensaje> tiposDeMensajeAdmitidos){
        this.tiposDeMensajeAdmitidos = tiposDeMensajeAdmitidos;
    }

    public MedioDeContacto() {}

    public abstract String getValor();

    public void enviarMensaje(Mensaje mensaje) {
        if (!tiposDeMensajeAdmitidos.contains(mensaje.getTipoDeMensaje())) {
            throw new IllegalArgumentException("Tipo de mensaje no permitido: " + mensaje.getTipoDeMensaje());
        }
    }

    public void agregarTipoDeMensajeAdmitido(TipoDeMensaje tipoDeMensaje){
        this.tiposDeMensajeAdmitidos.add(tipoDeMensaje);
    }

    public void eliminarTipoDeMensajeAdmitido(TipoDeMensaje tipoDeMensaje){
        this.tiposDeMensajeAdmitidos.remove(tipoDeMensaje);
    }


}
