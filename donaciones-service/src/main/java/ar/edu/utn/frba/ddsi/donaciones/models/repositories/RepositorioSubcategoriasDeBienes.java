package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre SubcategoriaBienJpaRepository (Spring Data JPA).
 */
@Repository
public class RepositorioSubcategoriasDeBienes {

    private final SubcategoriaBienJpaRepository jpaRepository;

    public RepositorioSubcategoriasDeBienes(SubcategoriaBienJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public SubcategoriaBien guardar(SubcategoriaBien subcategoria) {
        return jpaRepository.save(subcategoria);
    }

    public Optional<SubcategoriaBien> buscarPorNombreYCategoria(String nombre, String nombreCategoria) {
        return jpaRepository.findByNombreAndCategoriaNombre(nombre, nombreCategoria);
    }
}
