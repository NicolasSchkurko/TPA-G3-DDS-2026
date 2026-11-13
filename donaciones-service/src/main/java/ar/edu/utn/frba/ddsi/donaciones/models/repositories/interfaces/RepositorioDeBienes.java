package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositorio Spring Data JPA para Bien (jerarquía JOINED: BienConEstado/BienPerecedero).
 * No se usa directamente desde los Gestores/Services: RepositorioDeBienes actúa como fachada.
 */
public interface RepositorioDeBienes extends JpaRepository<Bien, UUID> {

  default void guardar(Bien bien) {
    if (bien != null) {
      // Aseguramos que tenga un ID si no se inicializó en su constructor
      if (bien.getId() == null) {
        bien.setId(UUID.randomUUID());
      }
      if (buscarPorId(bien.getId()).isPresent()) {
        throw new IllegalArgumentException("Ya existe un bien con el ID: " + bien.getId());
      }
      this.save(bien);
    }
  }

  default  Optional<Bien> buscarPorId(UUID id) {
    return this.findById(id);
  }

    default List<Bien> obtenerTodos() { return findAll(); }

    default void actualizar(UUID idOriginal, Bien bienActualizado) {
        if (existsById(idOriginal)) {
            save(bienActualizado);
        } else {
            throw new IllegalArgumentException("No se encontró el bien a actualizar.");
        }
    }

    default void eliminarPorId(UUID id) { deleteById(id); }

}

