package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para SubcategoriaBien.
 * RepositorioSubcategoriasDeBienes actúa como fachada sobre esta interfaz (ver EntidadBeneficiariaJpaRepository).
 */
public interface SubcategoriaBienJpaRepository extends JpaRepository<SubcategoriaBien, UUID> {
    Optional<SubcategoriaBien> findByNombreAndCategoriaNombre(String nombre, String categoriaNombre);
}
