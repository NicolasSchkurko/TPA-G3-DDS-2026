package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.choferes.RepositorioChoferes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class GestorChoferes {
    private final RepositorioChoferes repoChoferes;

    public GestorChoferes(RepositorioChoferes repoChoferes){
        this.repoChoferes = repoChoferes;
    }

    public List<Chofer>  listarChoferes(){
        return repoChoferes.findAll();
    }

    public Chofer buscarChofer(UUID id) {
        Optional<Chofer> chofer = repoChoferes.findById(id);

        if (chofer.isPresent()) {
            return chofer.get();
        } else {
            throw new IllegalArgumentException("Chofer no encontrado");
        }
    }

    public void guardarChofer(Chofer chofer){
        repoChoferes.save(chofer);
    }

    public void eliminarChofer(UUID id){
        Chofer chofer = buscarChofer(id);
        if(chofer != null){
            repoChoferes.deleteById(id);
        }
    }

    public void marcarDisponible(UUID id) {
        Chofer chofer = buscarChofer(id);
        chofer.disponible();
        guardarChofer(chofer);
    }

    public void marcarOcupado(UUID id) {
        Chofer chofer = buscarChofer(id);
        chofer.ocupado();
        guardarChofer(chofer);
    }
}
