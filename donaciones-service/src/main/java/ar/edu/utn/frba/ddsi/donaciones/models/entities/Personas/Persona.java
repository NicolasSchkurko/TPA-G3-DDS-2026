package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Persona {
    private MediosDeContacto mediosDeContacto;

    private String nombreDeUsuario;


    public Persona(String nombreDeUsuario) {
        this.nombreDeUsuario = nombreDeUsuario;
    }

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.mediosDeContacto.agregarMedioDeContacto(medioDeContacto);
    }

}