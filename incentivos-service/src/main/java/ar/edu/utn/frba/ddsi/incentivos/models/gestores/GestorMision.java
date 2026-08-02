package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioMisiones;

import java.util.ArrayList;
import java.util.List;

/**
 * @param repositorio gestiona las misiones existentes en el repositorio
 */
public record GestorMision(RepositorioMisiones repositorio) {

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
