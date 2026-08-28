package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioPersonas;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Gestor (Servicio) para manejar la lógica de negocio relacionada con las Personas.
 */
@Service
public class GestorPersonas {

  private final RepositorioPersonas repositorio;

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
   * Actualiza los datos de una persona existente. Ahora con lógica centralizada.
   */
  public void modificarPersona(UUID idOriginal, Persona datosNuevos) {
    Persona existente = obtenerPersona(idOriginal);
    if (existente == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + idOriginal);
    }

    existente.setMediosDeContacto(datosNuevos.getMediosDeContacto());
    existente.setId(datosNuevos.getId()); // Se actualiza por si es necesario, basado en el diseño original

    // Lógica propia de dominio extraída del Controller/Service
    if (existente instanceof Juridica pj && datosNuevos instanceof Juridica pjNuevos) {
      pj.setRazonSocial(pjNuevos.getRazonSocial());
      pj.setCuit(pjNuevos.getCuit());
    }

    try {
      repositorio.actualizar(idOriginal, existente);
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
        repositorio.actualizar(idPersona, persona);
        System.out.println("Medio de contacto agregado exitosamente a la persona: " + idPersona);
      } catch (IllegalStateException e) {
        System.err.println("Error al agregar medio de contacto: " + e.getMessage());
      }
    } else {
      throw new IllegalArgumentException("No se pudo agregar el medio de contacto: Persona no encontrada.");
    }
  }
}