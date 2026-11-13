package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para SubcategoriaBien.
 * RepositorioDeSubcategoriasDeBienes actúa como fachada sobre esta interfaz (ver RepositorioDeEntidadesBeneficiarias).
 *
 * Las búsquedas vienen en dos sabores a propósito:
 *  - exacta (findByNombreAndCategoriaNombre), que es la que usa el alta de catálogo para decidir
 *    si tiene que crear la fila;
 *  - laxa (...IgnoreCase...), que es la que usa la resolución de bienes de una donación. Acá no
 *    queremos crear nada: "frazadas" y "Frazadas" tienen que terminar en la MISMA subcategoría,
 *    porque Necesidad.esCompatibleCon compara los nombres con equals() y una diferencia de
 *    mayúsculas alcanza para que el asignador no encuentre la necesidad.
 */
public interface RepositorioDeSubcategoriasDeBienes extends JpaRepository<SubcategoriaBien, UUID> {
    Optional<SubcategoriaBien> findByNombreAndCategoriaNombre(String nombre, String categoriaNombre);

    Optional<SubcategoriaBien> findByNombreIgnoreCaseAndCategoriaNombreIgnoreCase(String nombre,
                                                                                  String categoriaNombre);

    /** Lista porque el mismo nombre de subcategoría puede estar colgado de varias categorías. */
    List<SubcategoriaBien> findByNombreIgnoreCase(String nombre);

    default SubcategoriaBien guardar(SubcategoriaBien subcategoria) { return save(subcategoria); }

    default Optional<SubcategoriaBien> buscarPorNombreYCategoria(String nombre, String nombreCategoria) {
        return findByNombreAndCategoriaNombre(nombre, nombreCategoria);
    }

    default Optional<SubcategoriaBien> buscarPorNombreYCategoriaIgnorandoCase(String nombre,
                                                                              String nombreCategoria) {
        return findByNombreIgnoreCaseAndCategoriaNombreIgnoreCase(nombre, nombreCategoria);
    }

    default List<SubcategoriaBien> buscarPorNombreIgnorandoCase(String nombre) {
        return findByNombreIgnoreCase(nombre);
    }

}
