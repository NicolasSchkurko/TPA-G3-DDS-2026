package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre NecesidadJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria.
 */
@Repository
public class RepositorioNecesidades {

  private final NecesidadJpaRepository jpaRepository;

  public RepositorioNecesidades(NecesidadJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  public void guardar(Necesidad necesidad) {
    if (necesidad != null) {
      // Aseguramos que tenga un ID si no se inicializó en su constructor
      if (necesidad.getId() == null) {
        necesidad.setId(UUID.randomUUID());
      }
      if (buscarPorId(necesidad.getId()).isPresent()) {
        throw new IllegalArgumentException("Ya existe un Necesidad con el ID: " + necesidad.getId());
      }
      jpaRepository.save(necesidad);
    }
  }

  public List<Necesidad> obtenerTodos() {
    return jpaRepository.findAll();
  }

  public Optional<Necesidad> buscarPorId(UUID id) {
    return jpaRepository.findById(id);
  }

  public void agregarDonacion(UUID necesidadId, Donacion donacion) {
    Necesidad necesidad = buscarPorId(necesidadId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la Necesidad con ID: " + necesidadId));

    // Nota: Donacion todavía no es una entidad JPA (Necesidad.donaciones es @Transient),
    // así que esto no persiste en la base todavía. Queda pendiente para cuando se persista ese dominio.
    necesidad.registrarDonacionAsignada(donacion);
  }

  public void actualizar(UUID idOriginal, Necesidad necesidadActualizado) {
    if (jpaRepository.existsById(idOriginal)) {
      jpaRepository.save(necesidadActualizado);
    } else {
      throw new IllegalArgumentException("No se encontró el Necesidad a actualizar.");
    }
  }

  public void eliminarPorId(UUID id) {
    jpaRepository.deleteById(id);
  }
}
