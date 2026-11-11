package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para Administrador.
 * No se usa directamente desde los Gestores/Services: RepositorioDeAdministradores actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface RepositorioDeAdministradores extends JpaRepository<Administrador, UUID> {

    default void guardar(Administrador administrador) {
        if (administrador != null) {
            if (existsById(administrador.getId())) {
                throw new IllegalArgumentException("Ya existe un administrador con el ID: " + administrador.getId());
            }
            save(administrador);
        }
    }

    default List<Administrador> obtenerTodos() { return findAll(); }

    default Optional<Administrador> buscarPorId(UUID id) { return findById(id); }

    default void actualizar(UUID idOriginal, Administrador adminActualizado) {
        if (existsById(idOriginal)) {
            adminActualizado.setId(idOriginal);
            save(adminActualizado);
        } else {
            throw new IllegalArgumentException("No se encontró el administrador a actualizar.");
        }
    }

    default void eliminarPorId(UUID id) { deleteById(id); }

}
