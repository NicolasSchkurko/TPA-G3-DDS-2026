package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre DonanteJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria.
 */
@Repository
public class RepositorioDonantes {

  private final DonanteJpaRepository jpaRepository;

  public RepositorioDonantes(DonanteJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  public void guardar(Donante donante) {
    if (donante != null && donante.getId() != null) {
      if (jpaRepository.existsById(donante.getId())) {
        throw new IllegalArgumentException("Ya existe un donante con el ID: " + donante.getId());
      }
      jpaRepository.save(donante);
    }
  }

  public List<Donante> obtenerTodos() {
    return jpaRepository.findAll();
  }

  public Optional<Donante> buscarPorId(UUID id) {
    return jpaRepository.findById(id);
  }

  public void actualizar(UUID idOriginal, Donante donanteActualizado) {
    if (jpaRepository.existsById(idOriginal)) {
      jpaRepository.save(donanteActualizado);
    } else {
      throw new IllegalArgumentException("No se encontró el donante a actualizar.");
    }
  }

  public void eliminarPorId(UUID id) {
    jpaRepository.deleteById(id);
  }
}