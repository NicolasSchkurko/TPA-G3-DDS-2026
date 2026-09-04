package ar.edu.utn.frba.ddsi.logisticas.models.repositories.items;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RepositorioItemEntrega {
    private final List<ItemEntrega> itemEntregas = new ArrayList<>();

    public List<ItemEntrega> findAll() {
        return new ArrayList<>(itemEntregas);
    }

    public ItemEntrega findById(UUID idDonacion) {
        return itemEntregas.stream()
                           .filter(r -> r.getIdDonacion().equals(idDonacion))
                           .findFirst()
                           .orElse(null);
    }

    public List<ItemEntrega> findByEstado(EstadoEntrega estado) {
        return itemEntregas.stream()
                .filter(r -> estado.equals(r.getEstado()))
                .toList();
    }

    public ItemEntrega save(ItemEntrega item) {
        int posicion = itemEntregas.indexOf(item);
        if (posicion != -1) {
            itemEntregas.set(posicion, item);
        } else {
            itemEntregas.add(item);
        }
        return item;
    }

    public void deleteById(UUID idDonacion) {
        itemEntregas.removeIf(r -> r.getIdDonacion().equals(idDonacion));
    }
}