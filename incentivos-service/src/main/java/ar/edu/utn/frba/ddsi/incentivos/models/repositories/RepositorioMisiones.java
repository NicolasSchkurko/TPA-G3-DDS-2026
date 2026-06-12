package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.ArrayList;
import java.util.List;

public class RepositorioMisiones {
    private static RepositorioMisiones instanciaUnica;
    private final List<Mision> misiones;

    private RepositorioMisiones() {
        this.misiones = new ArrayList<>();
    }

    public static RepositorioMisiones getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new RepositorioMisiones();
        }
        return instanciaUnica;
    }

    public void agregarMision(Mision misionNueva){
        if (misionNueva != null && !misiones.contains(misionNueva)){
            misiones.add(misionNueva);
        }
    }

    public List<Mision> listarTodas() {
        return List.copyOf(misiones);
    }
}
