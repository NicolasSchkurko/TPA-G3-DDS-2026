package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para CategoriaBien.
 * RepositorioDeCategoriasDeBienes actúa como fachada sobre esta interfaz (ver RepositorioDeEntidadesBeneficiarias).
 */
public interface RepositorioDeCategoriasDeBienes extends JpaRepository<CategoriaBien, UUID> {
    Optional<CategoriaBien> findByNombre(String nombre);

    default CategoriaBien guardar(CategoriaBien categoria) { return save(categoria); }

    default Optional<CategoriaBien> buscarPorNombre(String nombre) { return findByNombre(nombre); }

    default CategoriaBien obtenerOCrearCategoria(String nombre) {
        return buscarPorNombre(nombre).orElseGet(() -> guardar(new CategoriaBien(nombre)));
    }

}
