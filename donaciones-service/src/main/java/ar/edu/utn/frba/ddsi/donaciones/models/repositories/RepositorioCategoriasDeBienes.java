package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre CategoriaBienJpaRepository (Spring Data JPA).
 */
@Repository
public class RepositorioCategoriasDeBienes {

    private final CategoriaBienJpaRepository jpaRepository;

    public RepositorioCategoriasDeBienes(CategoriaBienJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public CategoriaBien guardar(CategoriaBien categoria) {
        return jpaRepository.save(categoria);
    }

    public Optional<CategoriaBien> buscarPorNombre(String nombre) {
        return jpaRepository.findByNombre(nombre);
    }
}
