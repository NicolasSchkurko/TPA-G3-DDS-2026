package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;

import java.util.List;

public class MediosDeContacto {
    private MedioDeContacto medioDeContactoPredeterminado;
    private List<MedioDeContacto> listaMediosDeContacto;

    public void setMedioDeContactoPredeterminado(MedioDeContacto medioDeContactoPredeterminado) {}

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto){
        this.listaMediosDeContacto.add(medioDeContacto);
    }

    public void eliminarMedioDeContacto(MedioDeContacto medioDeContacto){
        this.listaMediosDeContacto.remove(medioDeContacto);
    }

    public void enviarMensajeAMedios(Mensaje mensaje){}
}
