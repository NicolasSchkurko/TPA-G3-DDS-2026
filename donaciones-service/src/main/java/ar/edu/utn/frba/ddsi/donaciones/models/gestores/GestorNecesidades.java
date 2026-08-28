package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
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

  //Lo agregue para poder registrar donaciones
  public void agregarDonacionANecesidad(UUID necesidadId, Donacion donacion) {
    try {
      repositorio.agregarDonacion(necesidadId, donacion);
      System.out.println("Donación registrada con éxito en la necesidad: " + necesidadId);
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar donación en necesidad: " + e.getMessage());
    }
  }
}

