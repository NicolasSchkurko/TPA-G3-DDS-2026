package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter

public class GestorPersonas {
    private static GestorPersonas instanciaUnica;

    private static List<PersonaDonante> personas;

    private GestorPersonas() {
        this.personas = new ArrayList<>();
    }

    public static GestorPersonas getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new GestorPersonas();
        }
        return instanciaUnica;
    }

    public void agregarPersona(PersonaDonante persona) {
        if(!personas.contains(persona)){
            this.personas.add(persona);
        }
    }

    public void eliminarPersona(PersonaDonante persona) {
        if(personas.contains(persona)){
            this.personas.remove(persona);
        }
    }

    public static PersonaDonante personaExiste(String nombre){
        if(!personas.isEmpty()){
            for(PersonaDonante persona : personas){
                if(persona.darNombre().equals(nombre)){
                    return persona;
                }
            }
        }
        return null;
    }
}

