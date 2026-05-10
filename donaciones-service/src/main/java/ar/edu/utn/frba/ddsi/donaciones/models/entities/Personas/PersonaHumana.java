package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;

public class PersonaHumana extends PersonaDonante{
    private Humano persona;
    private Direccion direccion;

    public PersonaHumana(Humano persona, Direccion direccion) {
        this.persona = persona;
        this.direccion = direccion;
    }
}
