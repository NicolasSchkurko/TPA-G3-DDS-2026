package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.BienJpaRepository;


import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre BienJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria.
 */
@Repository
public class RepositorioBienes {

  private final BienJpaRepository jpaRepository;

  public RepositorioBienes(BienJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  public void guardar(Bien bien) {
    if (bien != null) {
      // Aseguramos que tenga un ID si no se inicializó en su constructor
      if (bien.getId() == null) {
        bien.setId(UUID.randomUUID());
      }
      // Va directo al repositorio de Spring Data y no por un buscarPorId() propio: ese
      // método se borró en el commit 86c6f93 (25/09) y la llamada quedó, con lo que el
      // módulo entero no compilaba desde esa fecha. existsById es lo mismo que el
      // findById(...).isPresent() que había antes, sin el Optional de por medio.
      if (jpaRepository.existsById(bien.getId())) {
        throw new IllegalArgumentException("Ya existe un bien con el ID: " + bien.getId());
      }
      jpaRepository.save(bien);
    }
  }

  public List<Bien> obtenerTodos() {
    return jpaRepository.findAll();
  }

  public Optional<Bien> buscarPorId(UUID id) {
    return jpaRepository.findById(id);
  }


  public void actualizar(UUID idOriginal, Bien bienActualizado) {
    if (jpaRepository.existsById(idOriginal)) {
      jpaRepository.save(bienActualizado);
    } else {
      throw new IllegalArgumentException("No se encontró el bien a actualizar.");
    }
  }

  public void eliminarPorId(UUID id) {
    jpaRepository.deleteById(id);
  }
}
