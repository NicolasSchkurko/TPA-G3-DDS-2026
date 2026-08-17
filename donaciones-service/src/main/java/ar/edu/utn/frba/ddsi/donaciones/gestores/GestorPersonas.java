package ar.edu.utn.frba.ddsi.donaciones.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioPersonas;
import java.util.List;
import java.util.UUID;

/**
 * Gestor (Servicio) para manejar la lógica de negocio relacionada con las Personas.
 */
public class GestorPersonas {

  private RepositorioPersonas repositorio;

  public GestorPersonas() {
    // Inicializamos el repo en memoria. En un futuro esto se puede inyectar.
    this.repositorio = new RepositorioPersonas();
  }

  /**
   * Registra una nueva persona en el sistema.
   */
  public void registrarPersona(Persona nuevaPersona) {
    try {
      repositorio.guardar(nuevaPersona);
      System.out.println("Persona registrada con éxito con ID: " + nuevaPersona.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar: " + e.getMessage());
    }
  }

  /**
   * Obtiene una persona por su ID.
   */
  public Persona obtenerPersona(UUID id) {
    return repositorio.buscarPorId(id)
                      .orElse(null); // Retorna null si no se encuentra
  }

  /**
   * Obtiene la lista completa de personas.
   */
  public List<Persona> listarTodasLasPersonas() {
    return repositorio.obtenerTodas();
  }

  /**
   * Actualiza los datos de una persona existente.
   */
  public void modificarPersona(UUID idOriginal, Persona datosNuevos) {
    try {
      repositorio.actualizar(idOriginal, datosNuevos);
      System.out.println("Persona actualizada con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar: " + e.getMessage());
    }
  }

  /**
   * Agrega un nuevo medio de contacto a una persona existente.
   */
  public void agregarMedioDeContactoAPersona(UUID idPersona, MedioDeContacto nuevoMedio) {
    Persona persona = obtenerPersona(idPersona);
    if (persona != null) {
      try {
        persona.agregarMedioDeContacto(nuevoMedio);
        System.out.println("Medio de contacto agregado exitosamente a la persona: " + idPersona);
      } catch (IllegalStateException e) {
        System.err.println("Error al agregar medio de contacto: " + e.getMessage());
      }
    } else {
      System.err.println("No se pudo agregar el medio de contacto: Persona no encontrada.");
    }
  }

  /**
   * Elimina una persona del sistema.
   */
  public void darDeBajaPersona(UUID id) {
    repositorio.eliminarPorId(id);
    System.out.println("Persona dada de baja (si existía).");
  }
}