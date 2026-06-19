package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioEntidadesBeneficiarias {
    private final List<EntidadBeneficiaria> entidades;

    public RepositorioEntidadesBeneficiarias() {
        this.entidades = new ArrayList<>();
    }

    public List<EntidadBeneficiaria> findAll() {
        return new ArrayList<>(entidades);
    }

    public Optional<EntidadBeneficiaria> findById(UUID id) {
        return entidades.stream()
                        .filter(e -> e.getId().equals(id))
                        .findFirst();
    }

    public EntidadBeneficiaria save(EntidadBeneficiaria entidad) {
        deleteById(entidad.getId()); // Si existe la actualiza (borra e inserta), si no, la agrega.
        entidades.add(entidad);
        return entidad;
    }

    public void deleteById(UUID id) {
        entidades.removeIf(e -> e.getId().equals(id));
    }
}