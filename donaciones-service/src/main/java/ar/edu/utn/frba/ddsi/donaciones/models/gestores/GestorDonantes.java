package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonantes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Gestor (Servicio) para manejar la lógica de negocio relacionada con los Donantes de forma aislada.
 */
@Service
public class GestorDonantes {

  private final RepositorioDonantes repositorio;

  public GestorDonantes() {
    this.repositorio = new RepositorioDonantes();
  }

  public void registrarDonante(Donante nuevoDonante) {
    try {
      repositorio.guardar(nuevoDonante);
      System.out.println("Donante registrado con éxito con ID: " + nuevoDonante.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar donante: " + e.getMessage());
    }
  }

  public Donante obtenerDonante(UUID id) {
    return repositorio.buscarPorId(id).orElse(null);
  }

  public List<Donante> listarTodosLosDonantes() {
    return repositorio.obtenerTodos();
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

  public void darDeBajaDonante(UUID id) {
    repositorio.eliminarPorId(id);
    System.out.println("Donante dado de baja (si existía).");
  }

  public void agregarFormularioADonante(UUID idDonante, Formulario nuevoFormulario) {
    Donante donante = obtenerDonante(idDonante);
    if (donante != null) {
      donante.agregarFormulario(nuevoFormulario);
      repositorio.actualizar(idDonante, donante);
      System.out.println("Formulario agregado con éxito al donante: " + donante.getPersona().getNombreDeUsuario());
    } else {
      System.err.println("No se pudo agregar formulario: Donante no encontrado.");
    }
  }
}