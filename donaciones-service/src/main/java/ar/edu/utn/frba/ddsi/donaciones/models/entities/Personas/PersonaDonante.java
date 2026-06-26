package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class PersonaDonante {
    private UUID id;
    private MediosDeContacto mediosDeContacto;
    private List<Formulario> formularios;
    private Direccion direccion;

    public PersonaDonante(Direccion direccion) {
        this.id = UUID.randomUUID();
        this.mediosDeContacto = new MediosDeContacto();
        this.formularios = new ArrayList<>();
        this.direccion = direccion;

    }

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.mediosDeContacto.agregarMedioDeContacto(medioDeContacto);
    }

    public void agregarFormulario(Formulario formulario){
        this.formularios.add(formulario);
    }

    public abstract String darNombre();
}