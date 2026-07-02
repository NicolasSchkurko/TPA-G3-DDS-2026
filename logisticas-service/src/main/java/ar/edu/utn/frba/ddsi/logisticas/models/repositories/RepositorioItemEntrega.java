package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioItemEntrega {
    private final List<ItemEntrega> itemEntregas = new ArrayList<>();

    public List<ItemEntrega> findAll() {
        return itemEntregas;
    }

    public List<ItemEntrega> findAllById(List<UUID> idDonacion) {
        return idDonacion.stream()
                .map(this::findById)
                .flatMap(Optional::stream)
                .toList();
    }

    public Optional<ItemEntrega> findById(UUID idDonacion) {
        return itemEntregas.stream()
                .filter(r -> r.getIdDonacion().equals(idDonacion))
                .findFirst();
    }

    public List<ItemEntrega> findByEstado(EstadoEntrega estado) {
        return itemEntregas.stream()
                .filter(r -> r.getEstado().equals(estado)).toList();
    }

    public void save(ItemEntrega item) {
        itemEntregas.add(item);
    }

    public void deleteById(UUID idDonacion) {
        itemEntregas.removeIf(r -> r.getIdDonacion().equals(idDonacion));
    }
}

