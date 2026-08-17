package ar.edu.utn.frba.ddsi.donaciones.models.entities.donador;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Donante {
    private UUID id;
    private List<Formulario> formularios;
    private Direccion direccion;
    private Persona persona;

    public Donante(Direccion direccion, Persona persona ) {
        this.id = UUID.randomUUID();
        this.formularios = new ArrayList<>();
        this.direccion = direccion;
        this.persona = persona;
    }



    public void agregarFormulario(Formulario formulario){
        this.formularios.add(formulario);
    }

    public abstract String darNombre();
}