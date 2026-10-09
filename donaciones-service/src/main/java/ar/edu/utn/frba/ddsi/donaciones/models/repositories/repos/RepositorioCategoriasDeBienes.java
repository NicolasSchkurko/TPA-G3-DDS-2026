package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.CategoriaBienJpaRepository;

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

    // Antes vivía en GestorNecesidades: busca la CategoriaBien por nombre; si no existe, la crea
    // y la persiste, para no duplicar entradas del catálogo de Bienes.
    public CategoriaBien obtenerOCrearCategoria(String nombre) {
        return buscarPorNombre(nombre).orElseGet(() -> guardar(new CategoriaBien(nombre)));
    }
}
