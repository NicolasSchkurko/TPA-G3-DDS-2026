package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.List;

public class RepositorioMisiones {
    List<Mision> misiones;

    public void agregarMision(Mision misionNueva){
        if(!misiones.contains(misionNueva)){
            misiones.add(misionNueva);
        }
    }
}
