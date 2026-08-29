package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
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

  public Necesidad modificarNecesidad(UUID idOriginal, Necesidad datosNuevos) {
    Necesidad existente = repositorio.buscarPorId(idOriginal).get();
    if (repositorio.buscarPorId(idOriginal).isEmpty()) {
      throw new IllegalArgumentException("No se encontró la entidad con ID: " + idOriginal);
    }

    existente.setCantidadObjetivo(datosNuevos.getCantidadObjetivo());
    existente.setDescripcion(datosNuevos.getDescripcion());
    existente.setSubcategoria(datosNuevos.getSubcategoria());

    if (datosNuevos instanceof NecesidadRecurrente) {
      ((NecesidadRecurrente) existente).setPlazoEnDias(((NecesidadRecurrente) datosNuevos).getPlazoEnDias());
    }

    try {
      repositorio.actualizar(idOriginal, existente);
      System.out.println("Necesidad actualizada con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar necesidad: " + e.getMessage());
    }

    return existente;
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

