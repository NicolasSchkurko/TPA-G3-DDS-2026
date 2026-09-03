package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonantes;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Gestor (Servicio) para manejar la lógica de negocio relacionada con los Donantes de forma aislada.
 */
@Service
public class GestorDonantes {

  private final RepositorioDonantes repositorio;

  public GestorDonantes(RepositorioDonantes repositorio) {
    this.repositorio = repositorio;
  }

  public Donante modificarDonante(UUID idOriginal, Donante datosNuevos) {
    Donante existente = obtenerDonante(idOriginal);
    if (existente == null) {
      throw new IllegalArgumentException("No se encontró el donante con ID: " + idOriginal);
    }

    existente.setDireccion(datosNuevos.getDireccion());

    try {
      repositorio.actualizar(idOriginal, existente);
      System.out.println("Donante actualizado con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar donante: " + e.getMessage());
    }

    return existente;
  }

  public void agregarFormularioADonante(UUID idDonante, Formulario nuevoFormulario) {
    Donante donante = repositorio.buscarPorId(idDonante).orElse(null);
    if (donante != null) {
      donante.agregarFormulario(nuevoFormulario);
      repositorio.actualizar(idDonante, donante);
      System.out.println("Formulario agregado con éxito al donante: " + donante.getPersona().getNombreDeUsuario());
    } else {
      System.err.println("No se pudo agregar formulario: Donante no encontrado.");
    }
  }

  private Donante obtenerDonante(UUID id) {
    return repositorio.buscarPorId(id).orElse(null);
  }
}