package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre EntidadBeneficiariaJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria,
 * para no tener que tocar los Gestores/Services que ya la usan.
 */
@Repository
public class RepositorioEntidadesBeneficiarias {

    private final EntidadBeneficiariaJpaRepository jpaRepository;

    public RepositorioEntidadesBeneficiarias(EntidadBeneficiariaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void guardar(EntidadBeneficiaria entidad) {
        if (entidad != null && entidad.getId() != null) {
            if (buscarPorId(entidad.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una entidad con el ID: " + entidad.getId());
            }
            jpaRepository.save(entidad);
        }
    }

    public List<EntidadBeneficiaria> obtenerTodas() {
        return jpaRepository.findAll();
    }

    public Optional<EntidadBeneficiaria> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    public void actualizar(UUID idOriginal, EntidadBeneficiaria entidadActualizada) {
        if (jpaRepository.existsById(idOriginal)) {
            jpaRepository.save(entidadActualizada);
        } else {
            throw new IllegalArgumentException("No se encontró la entidad a actualizar.");
        }
    }

    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }
}
