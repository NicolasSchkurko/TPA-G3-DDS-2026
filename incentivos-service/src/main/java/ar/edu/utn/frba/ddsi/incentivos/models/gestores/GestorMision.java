package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioMisiones;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class GestorMision {
    private final RepositorioMisiones repositorio;

    public GestorMision(RepositorioMisiones repositorio) {
        this.repositorio = repositorio;
    }

    public List<Mision> conseguirMisiones(List<String> nombreMisiones) {
        List<Mision> misiones = new ArrayList<>();

        for (String nombreMision : nombreMisiones) {
            List<Mision> lstMisiones = repositorio.obtenerTodas();
            for (Mision m : lstMisiones) {
                if (m.getNombreMision().equals(nombreMision)) {
                    misiones.add(m);
                }
            }
        }

        return misiones;
    }


}
