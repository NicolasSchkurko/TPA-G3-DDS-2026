package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import lombok.Setter;

import java.util.List;

public class MediosDeContacto {
    @Setter
    private MedioDeContacto medioDeContactoPredeterminado;
    private List<MedioDeContacto> listaMediosDeContacto;

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto){
        this.listaMediosDeContacto.add(medioDeContacto);
    }

    public void eliminarMedioDeContacto(MedioDeContacto medioDeContacto){
        this.listaMediosDeContacto.remove(medioDeContacto);
    }

    public void enviarMensajeAMedios(Mensaje mensaje){}
}
