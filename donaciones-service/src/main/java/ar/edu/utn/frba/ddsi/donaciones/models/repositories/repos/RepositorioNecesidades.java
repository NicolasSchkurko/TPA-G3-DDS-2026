package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.NecesidadJpaRepository;

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
  private final RepositorioDonaciones repositorioDonaciones;

  public RepositorioNecesidades(NecesidadJpaRepository jpaRepository, RepositorioDonaciones repositorioDonaciones) {
    this.jpaRepository = jpaRepository;
    this.repositorioDonaciones = repositorioDonaciones;
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

    // El dueño de la relación es Donacion.necesidad (columna necesidad_id), así que el cambio
    // se persiste guardando la Donacion, no la Necesidad.
    necesidad.registrarDonacionAsignada(donacion);
    repositorioDonaciones.guardar(donacion);
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

  // Antes vivía en GestorNecesidades: es manejo de dominio de la propia Necesidad,
  // se movió acá para no mantener un gestor que solo delegaba en el repositorio.
  public Necesidad modificarNecesidad(UUID idOriginal, Necesidad datosNuevos) {
    Necesidad existente = buscarPorId(idOriginal)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad con ID: " + idOriginal));

    existente.setCantidadObjetivo(datosNuevos.getCantidadObjetivo());
    existente.setDescripcion(datosNuevos.getDescripcion());
    existente.setSubcategoria(datosNuevos.getSubcategoria());

    if (datosNuevos instanceof NecesidadRecurrente) {
      ((NecesidadRecurrente) existente).setPlazoEnDias(((NecesidadRecurrente) datosNuevos).getPlazoEnDias());
    }

    try {
      actualizar(idOriginal, existente);
      System.out.println("Necesidad actualizada con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar necesidad: " + e.getMessage());
    }

    return existente;
  }
}
