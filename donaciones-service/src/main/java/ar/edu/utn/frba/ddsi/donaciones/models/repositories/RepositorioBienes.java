package ar.edu.utn.frba.ddsi.donaciones.models.repositories;


import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioBienes {
  private List<Bien> bienesEnMemoria;;

  public RepositorioBienes() {
    this.bienesEnMemoria = new ArrayList<>();
  }

  public void guardar(Bien bien) {
    if (bien != null) {
      // Aseguramos que tenga un ID si no se inicializó en su constructor
      if (bien.getId() == null) {
        bien.setId(UUID.randomUUID());
      }
      if (buscarPorId(bien.getId()).isPresent()) {
        throw new IllegalArgumentException("Ya existe un bien con el ID: " + bien.getId());
      }
      this.bienesEnMemoria.add(bien);
    }
  }

  public List<Bien> obtenerTodos() {
    return new ArrayList<>(this.bienesEnMemoria);
  }

  public Optional<Bien> buscarPorId(UUID id) {
    return this.bienesEnMemoria.stream()
                                        .filter(a -> a.getId().equals(id))
                                        .findFirst();
  }

  public void actualizar(UUID idOriginal, Bien bienActualizado) {
    Optional<Bien> bienExistente = buscarPorId(idOriginal);
    if (bienExistente.isPresent()) {
      int index = this.bienesEnMemoria.indexOf(bienExistente.get());
      this.bienesEnMemoria.set(index, bienActualizado);
    } else {
      throw new IllegalArgumentException("No se encontró el bien a actualizar.");
    }
  }

  public void eliminarPorId(UUID id) {
    this.bienesEnMemoria.removeIf(a -> a.getId().equals(id));
  }
}
