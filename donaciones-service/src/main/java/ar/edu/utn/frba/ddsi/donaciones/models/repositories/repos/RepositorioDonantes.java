package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.DonanteJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
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

  // --- Antes vivían en GestorDonantes: son manejo de dominio de la propia Donante,
  // se movieron acá para no mantener un gestor que solo delegaba en el repositorio. ---

  public Donante modificarDonante(UUID idOriginal, Donante datosNuevos) {
    Donante existente = buscarPorId(idOriginal).orElse(null);
    if (existente == null) {
      throw new IllegalArgumentException("No se encontró el donante con ID: " + idOriginal);
    }

    existente.setDireccion(datosNuevos.getDireccion());

    try {
      actualizar(idOriginal, existente);
      System.out.println("Donante actualizado con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar donante: " + e.getMessage());
    }

    return existente;
  }

  public void agregarFormularioADonante(UUID idDonante, Formulario nuevoFormulario) {
    Donante donante = buscarPorId(idDonante).orElse(null);
    if (donante != null) {
      donante.agregarFormulario(nuevoFormulario);
      actualizar(idDonante, donante);
      System.out.println("Formulario agregado con éxito al donante: " + donante.getPersona().getNombreDeUsuario());
    } else {
      System.err.println("No se pudo agregar formulario: Donante no encontrado.");
    }
  }
}