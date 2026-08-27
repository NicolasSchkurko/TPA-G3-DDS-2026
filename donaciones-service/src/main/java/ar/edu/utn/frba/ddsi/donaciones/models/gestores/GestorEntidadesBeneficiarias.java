package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestor (Servicio) para manejar la lógica de negocio relacionada con Entidades Beneficiarias de forma aislada.
 */
@Service
public class GestorEntidadesBeneficiarias {

  private final RepositorioEntidadesBeneficiarias repositorio;

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

  public EntidadBeneficiaria modificarEntidad(UUID idOriginal, EntidadBeneficiaria datosNuevos) {
    EntidadBeneficiaria existente = obtenerEntidad(idOriginal);
    if (existente == null) {
      throw new IllegalArgumentException("No se encontró la entidad con ID: " + idOriginal);
    }
    existente.setDireccion(datosNuevos.getDireccion());

    try {
      repositorio.actualizar(idOriginal, existente);
      System.out.println("Entidad beneficiaria actualizada con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar entidad: " + e.getMessage());
    }

    return existente;
  }

  public void darDeBajaEntidad(UUID id) {
    repositorio.eliminarPorId(id);
    System.out.println("Entidad beneficiaria dada de baja (si existía).");
  }

  public void agregarNecesidadAEntidad(UUID idEntidad, Necesidad nuevaNecesidad) {
    EntidadBeneficiaria entidad = obtenerEntidad(idEntidad);
    if (entidad != null) {
      entidad.agregarNecesidad(nuevaNecesidad);
      repositorio.actualizar(idEntidad, entidad);
      System.out.println("Necesidad agregada a la entidad: " + idEntidad);
    } else {
      throw new IllegalArgumentException("No se pudo agregar necesidad: Entidad no encontrada.");
    }
  }

  public void eliminarNecesidadDeEntidad(UUID idEntidad, UUID idNecesidad) {
    EntidadBeneficiaria entidad = obtenerEntidad(idEntidad);
    if (entidad == null) {
      throw new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad);
    }

    Necesidad necesidad = entidad.buscarNecesidadPorId(idNecesidad)
                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la necesidad con ID: " + idNecesidad));

    entidad.eliminarNecesidad(necesidad);
    repositorio.actualizar(idEntidad, entidad);
    System.out.println("Necesidad desvinculada de la entidad con éxito.");
  }

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