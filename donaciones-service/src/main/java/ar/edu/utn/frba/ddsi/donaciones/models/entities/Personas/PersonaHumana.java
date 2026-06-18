package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonaHumana extends PersonaDonante {

    private Humano persona;

    public PersonaHumana(Humano persona, Direccion direccion) {
        super(direccion);
        this.persona = persona;
    }

    public String darNombre() {
        return persona.getNombre() + " " + persona.getApellido();
    }

    @Override
    public String toString() {
        return "PersonaHumana{persona=" + persona + ", direccion=" + this.getDireccion() + '}';
    }
}