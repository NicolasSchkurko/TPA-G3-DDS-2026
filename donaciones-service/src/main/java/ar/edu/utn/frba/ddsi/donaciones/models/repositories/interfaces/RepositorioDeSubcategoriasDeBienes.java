package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para SubcategoriaBien.
 * RepositorioDeSubcategoriasDeBienes actúa como fachada sobre esta interfaz (ver RepositorioDeEntidadesBeneficiarias).
 */
public interface RepositorioDeSubcategoriasDeBienes extends JpaRepository<SubcategoriaBien, UUID> {
    Optional<SubcategoriaBien> findByNombreAndCategoriaNombre(String nombre, String categoriaNombre);

    default SubcategoriaBien guardar(SubcategoriaBien subcategoria) { return save(subcategoria); }

    default Optional<SubcategoriaBien> buscarPorNombreYCategoria(String nombre, String nombreCategoria) {
        return findByNombreAndCategoriaNombre(nombre, nombreCategoria);
    }

}
