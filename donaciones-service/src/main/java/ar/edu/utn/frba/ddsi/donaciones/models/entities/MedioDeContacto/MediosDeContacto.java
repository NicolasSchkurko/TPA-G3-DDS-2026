package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class MediosDeContacto {
    private MedioDeContacto medioDeContactoPredeterminado;

    // Se inicializa la lista para evitar el NullPointerException
    private List<MedioDeContacto> listaMediosDeContacto = new ArrayList<>();

    public MediosDeContacto() {
    }

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.listaMediosDeContacto.add(medioDeContacto);
    }

    public void eliminarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.listaMediosDeContacto.remove(medioDeContacto);
    }

    public void enviarMensajeAMedios(Mensaje mensaje) {}
}