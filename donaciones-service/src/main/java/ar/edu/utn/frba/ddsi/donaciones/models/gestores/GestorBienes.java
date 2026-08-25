package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioBienes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GestorBienes {

  private RepositorioBienes repositorio;

  public GestorBienes() {
    this.repositorio = new RepositorioBienes();
  }

  public void crearBien(Bien nuevoBien) {
    try {
      repositorio.guardar(nuevoBien);
      System.out.println("Bien registrado con éxito con ID: " + nuevoBien.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar bien: " + e.getMessage());
    }
  }

  public Optional<Bien> obtenerBienPorId(UUID id) {
    return Optional.ofNullable(repositorio.buscarPorId(id)
                                          .orElse(null));
  }

  public List<Bien> obtenerTodosLosBienes() {
    return repositorio.obtenerTodos();
  }

  public void actualizarBien(UUID idOriginal, Bien datosNuevos) {
    try {
      repositorio.actualizar(idOriginal, datosNuevos);
      System.out.println("Administrador actualizado con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar administrador: " + e.getMessage());
    }
  }

  public void eliminarBien(UUID id) {
    repositorio.eliminarPorId(id);
    System.out.println("Administrador dado de baja (si existía).");
  }
}

