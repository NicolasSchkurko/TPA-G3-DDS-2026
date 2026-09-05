package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para CategoriaBien.
 * RepositorioCategoriasDeBienes actúa como fachada sobre esta interfaz (ver EntidadBeneficiariaJpaRepository).
 */
public interface CategoriaBienJpaRepository extends JpaRepository<CategoriaBien, UUID> {
    Optional<CategoriaBien> findByNombre(String nombre);
}
