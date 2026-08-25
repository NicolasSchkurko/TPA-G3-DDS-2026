package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioNecesidades;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GestorNecesidades {

  private RepositorioNecesidades repositorio;

  public GestorNecesidades() {
    this.repositorio = new RepositorioNecesidades();
  }

  public void crearNecesidad(Necesidad nuevoNecesidad) {
    try {
      repositorio.guardar(nuevoNecesidad);
      System.out.println("Necesidad registrada con éxito con ID: " + nuevoNecesidad.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar Necesidad: " + e.getMessage());
    }
  }

  public Optional<Necesidad> obtenerNecesidadPorId(UUID id) {
    return Optional.ofNullable(repositorio.buscarPorId(id)
                                          .orElse(null));
  }

  public List<Necesidad> obtenerTodosLasNecesidades() {
    return repositorio.obtenerTodos();
  }

  public void actualizarNecesidad(UUID idOriginal, Necesidad datosNuevos) {
    try {
      repositorio.actualizar(idOriginal, datosNuevos);
      System.out.println("Administrador actualizado con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar administrador: " + e.getMessage());
    }
  }

  public void eliminarNecesidad(UUID id) {
    repositorio.eliminarPorId(id);
    System.out.println("Administrador dado de baja (si existía).");
  }
}

