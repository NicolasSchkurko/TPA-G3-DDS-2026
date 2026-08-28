package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repositorio en memoria para gestionar operaciones CRUD sobre Entidades Beneficiarias.
 */
@Repository
public class RepositorioEntidadesBeneficiarias {
    private List<EntidadBeneficiaria> entidadesEnMemoria;

    public RepositorioEntidadesBeneficiarias() {
        this.entidadesEnMemoria = new ArrayList<>();
    }

    public void guardar(EntidadBeneficiaria entidad) {
        if (entidad != null && entidad.getId() != null) {
            if (buscarPorId(entidad.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una entidad con el ID: " + entidad.getId());
            }
            this.entidadesEnMemoria.add(entidad);
        }
    }

    public List<EntidadBeneficiaria> obtenerTodas() {
        return new ArrayList<>(this.entidadesEnMemoria);
    }

    public Optional<EntidadBeneficiaria> buscarPorId(UUID id) {
        return this.entidadesEnMemoria.stream()
                                      .filter(e -> e.getId().equals(id))
                                      .findFirst();
    }

    public void actualizar(UUID idOriginal, EntidadBeneficiaria entidadActualizada) {
        Optional<EntidadBeneficiaria> entidadExistente = buscarPorId(idOriginal);
        if (entidadExistente.isPresent()) {
            int index = this.entidadesEnMemoria.indexOf(entidadExistente.get());
            this.entidadesEnMemoria.set(index, entidadActualizada);
        } else {
            throw new IllegalArgumentException("No se encontró la entidad a actualizar.");
        }
    }

    public void eliminarPorId(UUID id) {
        this.entidadesEnMemoria.removeIf(e -> e.getId().equals(id));
    }
}
