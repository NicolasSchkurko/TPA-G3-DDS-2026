package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import lombok.Setter;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter

public abstract class PersonaDonante {
    private MediosDeContacto mediosDeContacto;
    private List<Formulario> formularios;

    /*public PersonaDonante(MediosDeContacto mediosDeContacto){
        this.mediosDeContacto = mediosDeContacto;
    }*/
    public PersonaDonante() {
        this.mediosDeContacto = new MediosDeContacto();
        this.formularios = new ArrayList<>();
    }

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto){
        this.mediosDeContacto.agregarMedioDeContacto(medioDeContacto);
    }

    public void agregarFormulario(Formulario formulario){
        this.formularios.add(formulario);
    }

    public abstract String darNombre();
}
