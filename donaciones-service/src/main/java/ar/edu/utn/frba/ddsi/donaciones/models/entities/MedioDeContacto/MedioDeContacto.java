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
    //public List<TipoDeMensaje> getTiposDeMensajeAdmitidos() {}
    /*
    public void agregarTipoDeMensajeAdmitido(TipoDeMensaje tipoDeMensaje){
        this.tiposDeMensajeAdmitidos.add(tipoDeMensaje);
    }

    public void eliminarTipoDeMensajeAdmitido(TipoDeMensaje tipoDeMensaje){
        this.tiposDeMensajeAdmitidos.remove(tipoDeMensaje);
    }
    */
    public void setInformacionDeContacto() {}

    public abstract void enviarMensaje(String mensaje, TipoDeMensaje tipo);

}
