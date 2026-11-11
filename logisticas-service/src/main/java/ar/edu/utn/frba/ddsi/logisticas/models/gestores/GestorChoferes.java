package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GestorChoferes {

    private final RepositorioChoferes repoChoferes;
    private final RepositorioCamiones repoCamiones;

    public GestorChoferes(RepositorioChoferes repoChoferes, RepositorioCamiones repoCamiones) {
        this.repoChoferes = repoChoferes;
        this.repoCamiones = repoCamiones;
    }

    public Chofer actualizar(UUID id, String nombre, Boolean disponible) {
        Chofer chofer = buscarActivo(id);
        if (nombre != null) {
            chofer.setNombre(nombre);
        }
        if (disponible != null) {
            chofer.setDisponible(disponible);
        }
        repoChoferes.save(chofer);
        return chofer;
    }

    public void darDeBaja(UUID id) {
        Chofer chofer = buscarActivo(id);
        chofer.darDeBaja();
        repoChoferes.save(chofer);

        repoCamiones.findByChofer_IdChofer(id).ifPresent(camion -> {
            camion.eliminarChofer();
            repoCamiones.save(camion);
        });
    }

    public void cambiarDisponibilidad(UUID id, boolean disponible) {
        Chofer chofer = buscarActivo(id);
        if (disponible) {
            chofer.disponible();
        } else {
            chofer.ocupado();
        }
        repoChoferes.save(chofer);
    }

    private Chofer buscarActivo(UUID id) {
        return repoChoferes.findById(id)
                .filter(Chofer::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Chofer no encontrado"));
    }
}
