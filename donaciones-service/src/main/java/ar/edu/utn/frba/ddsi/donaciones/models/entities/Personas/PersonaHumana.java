package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import lombok.Getter;
import lombok.Setter;

public class PersonaHumana extends PersonaDonante{

    @Getter
    @Setter
    private Humano persona;
    private Direccion direccion;

    public PersonaHumana(Humano persona, Direccion direccion) {
        //super(mediosDeContacto);
        super();
        this.direccion = direccion;

    }

    public String darNombre(){
        return persona.getNombre();
    }

    @Override
    public String toString() {
        return "PersonaHumana{persona=" + persona + ", direccion=" + direccion + '}';
    }
}
