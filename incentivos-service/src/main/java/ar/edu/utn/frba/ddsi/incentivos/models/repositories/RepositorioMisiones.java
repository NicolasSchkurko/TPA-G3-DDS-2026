package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RepositorioMisiones {
    private final List<Mision> misiones;

    public RepositorioMisiones() {
        this.misiones = new ArrayList<>();
    }

    public void agregarMision(Mision mision) {
        if (!misiones.contains(mision)) {
            misiones.add(mision);
        }
    }

    public void eliminarMision(Mision mision) {
        misiones.remove(mision);
    }

    public Mision actualizar(Mision misionModificada) {
        if (misionModificada == null) {
            return null;
        }

        Mision existente = this.buscarPorId(misionModificada.getIdMision());

        if (existente != null) {
            int index = misiones.indexOf(existente);
            if (index >= 0) {
                misiones.set(index, existente);
            }
            return existente;
        }

        return null;
    }

    public Mision buscarPorId(UUID id) {
        if (id == null || misiones.isEmpty()) return null;

        return misiones.stream()
                .filter(m -> id.equals(m.getIdMision()))
                .findFirst()
                .orElse(null);
    }

    public List<Mision> obtenerTodas() {
        return new ArrayList<>(this.misiones);
    }
}
