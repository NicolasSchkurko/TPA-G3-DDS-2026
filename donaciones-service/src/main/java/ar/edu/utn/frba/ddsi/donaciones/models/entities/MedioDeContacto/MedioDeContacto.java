package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;

import java.util.List;

public abstract class MedioDeContacto {
    private List<TipoDeMensaje> tiposDeMensajeAdmitidos;

    public void agregarTipoDeMensajeAdmitido(TipoDeMensaje tipoDeMensaje){
        this.tiposDeMensajeAdmitidos.add(tipoDeMensaje);
    }

    public void eliminarTipoDeMensajeAdmitido(TipoDeMensaje tipoDeMensaje){
        this.tiposDeMensajeAdmitidos.remove(tipoDeMensaje);
    }

    public void setInformacionDeContacto() {}

    public void enviarMensaje(Mensaje mensaje) {}
}
