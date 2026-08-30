package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import org.springframework.stereotype.Service;

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

  private EntidadBeneficiaria obtenerEntidad(UUID id) {
    return repositorio.buscarPorId(id).orElse(null);
  }
}