package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioBienes;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
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

  public void actualizarBien(UUID idOriginal, Bien datosNuevos) {
    try {
      repositorio.actualizar(idOriginal, datosNuevos);
      System.out.println("Administrador actualizado con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar administrador: " + e.getMessage());
    }
  }
}

