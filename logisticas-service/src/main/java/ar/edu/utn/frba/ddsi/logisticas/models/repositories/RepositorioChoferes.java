package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioChoferes {
    private final List<Chofer> choferes = new ArrayList<>();

    public List<Chofer> findAll() {
        return choferes;
    }

    public Chofer findById(UUID idChofer) {
        return choferes.stream()
                .filter(chofer -> chofer.getIdChofer().equals(idChofer))
                .findFirst()
                .orElse(null);
    }

    public void add(Chofer chofer) {
        this.choferes.add(chofer);
    }

    public void addAll(List<Chofer> choferes) {
        this.choferes.addAll(choferes);
    }

    public void actualizarCamion(Chofer chofer, Camion camion){
        int posicion = choferes.indexOf(chofer);
        chofer.setCamionAsignado(camion);
        choferes.set(posicion, chofer);
    }

    public void actualizarCamion(Chofer chofer){
        int posicion = choferes.indexOf(chofer);
        chofer.setCamionAsignado(null);
        choferes.set(posicion, chofer);
    }
}
