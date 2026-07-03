package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RepositorioChoferes {
    private final List<Chofer> choferes = new ArrayList<>();

    public List<Chofer> findAll() {
        return new ArrayList<>(choferes);
    }

    public Chofer findById(UUID idChofer) {
        return choferes.stream()
                       .filter(chofer -> chofer.getIdChofer().equals(idChofer))
                       .findFirst()
                       .orElse(null);
    }

    public Chofer save(Chofer chofer) {
        int posicion = choferes.indexOf(chofer);
        if (posicion != -1) {
            choferes.set(posicion, chofer);
        } else {
            choferes.add(chofer);
        }
        return chofer;
    }

    public void addAll(List<Chofer> nuevosChoferes) {
        this.choferes.addAll(nuevosChoferes);
    }

    public void deleteById(UUID idChofer) {
        choferes.removeIf(c -> c.getIdChofer().equals(idChofer));
    }
}