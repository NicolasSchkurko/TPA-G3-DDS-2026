package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class RepositorioItemEntrega {
    private final List<ItemEntrega> itemEntregas = new ArrayList<>();

    public List<ItemEntrega> findAll() {
        return new ArrayList<>(itemEntregas);
    }

    public List<ItemEntrega> findAllById(List<UUID> idDonacion) {
        return idDonacion.stream()
                         .map(this::findById)
                         .filter(Objects::nonNull)
                         .toList();
    }

    public ItemEntrega findById(UUID idDonacion) {
        return itemEntregas.stream()
                           .filter(r -> r.getIdDonacion().equals(idDonacion))
                           .findFirst()
                           .orElse(null);
    }

    public List<ItemEntrega> findByEstado(EstadoEntrega estado) {
        return itemEntregas.stream()
                           .filter(r -> r.getEstado().equals(estado)).toList();
    }

    public void save(ItemEntrega item) {
        int posicion = itemEntregas.indexOf(item);
        if (posicion != -1) {
            itemEntregas.set(posicion, item); // Actualiza si ya existe
        } else {
            itemEntregas.add(item); // Agrega si es nuevo
        }
    }

    public void actualizarEstado(ItemEntrega item, EstadoEntrega nuevoEstado){
        int posicion = itemEntregas.indexOf(item);
        item.setEstado(nuevoEstado);
        if (posicion != -1) {
            itemEntregas.set(posicion, item);
        }
    }

    public void deleteById(UUID idDonacion) {
        itemEntregas.removeIf(r -> r.getIdDonacion().equals(idDonacion));
    }
}