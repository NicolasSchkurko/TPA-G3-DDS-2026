package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repositorio en memoria para gestionar operaciones CRUD sobre objetos Donante.
 * Almacena instancias concretas de clases que hereden de la clase abstracta Donante.
 */
@Repository
public class RepositorioDonantes {
  private List<Donante> donantesEnMemoria;

  public RepositorioDonantes() {
    this.donantesEnMemoria = new ArrayList<>();
  }

  public void guardar(Donante donante) {
    if (donante != null && donante.getId() != null) {
      if (buscarPorId(donante.getId()).isPresent()) {
        throw new IllegalArgumentException("Ya existe un donante con el ID: " + donante.getId());
      }
      this.donantesEnMemoria.add(donante);
    }
  }

  public List<Donante> obtenerTodos() {
    return new ArrayList<>(this.donantesEnMemoria);
  }

  public Optional<Donante> buscarPorId(UUID id) {
    return this.donantesEnMemoria.stream()
                                 .filter(d -> d.getId().equals(id))
                                 .findFirst();
  }

  public void actualizar(UUID idOriginal, Donante donanteActualizado) {
    Optional<Donante> donanteExistente = buscarPorId(idOriginal);
    if (donanteExistente.isPresent()) {
      int index = this.donantesEnMemoria.indexOf(donanteExistente.get());
      this.donantesEnMemoria.set(index, donanteActualizado);
    } else {
      throw new IllegalArgumentException("No se encontró el donante a actualizar.");
    }
  }

  public void eliminarPorId(UUID id) {
    this.donantesEnMemoria.removeIf(d -> d.getId().equals(id));
  }
}