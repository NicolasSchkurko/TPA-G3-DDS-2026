package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestor (Servicio) para manejar la lógica de negocio relacionada con Entidades Beneficiarias.
 */
public class GestorEntidadesBeneficiarias {

  private RepositorioEntidadesBeneficiarias repositorio;

  public GestorEntidadesBeneficiarias() {
    this.repositorio = new RepositorioEntidadesBeneficiarias();
  }

  public void registrarEntidad(EntidadBeneficiaria nuevaEntidad) {
    try {
      repositorio.guardar(nuevaEntidad);
      System.out.println("Entidad beneficiaria registrada con éxito con ID: " + nuevaEntidad.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar entidad: " + e.getMessage());
    }
  }

  public EntidadBeneficiaria obtenerEntidad(UUID id) {
    return repositorio.buscarPorId(id).orElse(null);
  }

  public List<EntidadBeneficiaria> listarTodasLasEntidades() {
    return repositorio.obtenerTodas();
  }

  public void modificarEntidad(UUID idOriginal, EntidadBeneficiaria datosNuevos) {
    try {
      repositorio.actualizar(idOriginal, datosNuevos);
      System.out.println("Entidad beneficiaria actualizada con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar entidad: " + e.getMessage());
    }
  }

  public void darDeBajaEntidad(UUID id) {
    repositorio.eliminarPorId(id);
    System.out.println("Entidad beneficiaria dada de baja (si existía).");
  }

  /**
   * Agrega una necesidad a una entidad beneficiaria.
   */
  public void agregarNecesidadAEntidad(UUID idEntidad, Necesidad nuevaNecesidad) {
    EntidadBeneficiaria entidad = obtenerEntidad(idEntidad);
    if (entidad != null) {
      entidad.agregarNecesidad(nuevaNecesidad);
      System.out.println("Necesidad agregada a la entidad: " + idEntidad);
    } else {
      System.err.println("No se pudo agregar necesidad: Entidad no encontrada.");
    }
  }

  /**
   * Obtiene una lista unificada de todas las donaciones asociadas a las necesidades de la entidad.
   */
  public List<Donacion> obtenerDonacionesDeEntidad(UUID idEntidad) {
    EntidadBeneficiaria entidad = obtenerEntidad(idEntidad);
    if (entidad != null) {
      return entidad.verDonaciones();
    } else {
      System.err.println("Entidad no encontrada.");
      return new ArrayList<>();
    }
  }
}