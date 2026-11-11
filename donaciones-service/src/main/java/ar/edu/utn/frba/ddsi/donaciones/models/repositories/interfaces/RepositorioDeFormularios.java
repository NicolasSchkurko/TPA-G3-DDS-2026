package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repositorio Spring Data JPA para Formulario.
 * No se usa directamente desde los Gestores/Services: RepositorioDeFormularios actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface RepositorioDeFormularios extends JpaRepository<Formulario, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Formulario f SET f.donante = null WHERE f.donante.id = :donanteId")
    void desvincularDonante(@Param("donanteId") UUID donanteId);

    default void guardar(Formulario formulario) {
        if (formulario != null && formulario.getId() != null) {
            if (existsById(formulario.getId())) {
                throw new IllegalArgumentException("Ya existe un formulario con el ID: " + formulario.getId());
            }
            save(formulario);
        }
    }

    default List<Formulario> obtenerTodos() { return findAll(); }

    default Page<Formulario> obtenerTodos(Pageable pageable) { return findAll(pageable); }

    default Optional<Formulario> buscarPorId(UUID id) { return findById(id); }

    default void eliminarPorId(UUID id) { deleteById(id); }

}
