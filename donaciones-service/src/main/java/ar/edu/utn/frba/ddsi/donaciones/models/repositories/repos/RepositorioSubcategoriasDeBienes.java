package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.SubcategoriaBienJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre SubcategoriaBienJpaRepository (Spring Data JPA).
 */
@Repository
public class RepositorioSubcategoriasDeBienes {

    private final SubcategoriaBienJpaRepository jpaRepository;
    private final RepositorioCategoriasDeBienes repositorioCategorias;

    public RepositorioSubcategoriasDeBienes(SubcategoriaBienJpaRepository jpaRepository, RepositorioCategoriasDeBienes repositorioCategorias) {
        this.jpaRepository = jpaRepository;
        this.repositorioCategorias = repositorioCategorias;
    }

    public SubcategoriaBien guardar(SubcategoriaBien subcategoria) {
        return jpaRepository.save(subcategoria);
    }

    public Optional<SubcategoriaBien> buscarPorNombreYCategoria(String nombre, String nombreCategoria) {
        return jpaRepository.findByNombreAndCategoriaNombre(nombre, nombreCategoria);
    }

    // Antes vivía en GestorNecesidades: busca una SubcategoriaBien por nombre (dentro de su
    // CategoriaBien); si no existe, crea la CategoriaBien (si hace falta) y la SubcategoriaBien
    // y las persiste. Evita que cada Necesidad/Bien creado duplique entradas del catálogo.
    public SubcategoriaBien obtenerOCrearSubcategoria(String nombreCategoria, String nombreSubcategoria) {
        String catNombre = (nombreCategoria != null && !nombreCategoria.isBlank()) ? nombreCategoria : "General";
        String subNombre = (nombreSubcategoria != null && !nombreSubcategoria.isBlank()) ? nombreSubcategoria : "General";

        return buscarPorNombreYCategoria(subNombre, catNombre)
                .orElseGet(() -> {
                    CategoriaBien categoria = repositorioCategorias.obtenerOCrearCategoria(catNombre);
                    return guardar(new SubcategoriaBien(subNombre, categoria));
                });
    }
}
