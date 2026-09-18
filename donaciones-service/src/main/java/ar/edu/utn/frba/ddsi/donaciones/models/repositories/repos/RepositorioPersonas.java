package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.PersonaJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre PersonaJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria.
 */
@Repository
public class RepositorioPersonas {

    private final PersonaJpaRepository jpaRepository;

    public RepositorioPersonas(PersonaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    // Create
    public void guardar(Persona persona) {
        if (persona != null && persona.getId() != null) {
            // Verificamos si ya existe para evitar duplicados por ID
            if (buscarPorId(persona.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una persona con el ID: " + persona.getId());
            }
            jpaRepository.save(persona);
        }
    }

    // Read
    public List<Persona> obtenerTodas() {
        return jpaRepository.findAll();
    }

    public Optional<Persona> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    // Update
    public void actualizar(UUID idOriginal, Persona personaActualizada) {
        if (jpaRepository.existsById(idOriginal)) {
            jpaRepository.save(personaActualizada);
        } else {
            throw new IllegalArgumentException("No se encontró la persona a actualizar.");
        }
    }

    // Delete
    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }

    // --- Antes vivían en GestorPersonas: son manejo de dominio de la propia Persona,
    // se movieron acá para no mantener un gestor que solo delegaba en el repositorio. ---

    public void registrarPersona(Persona nuevaPersona) {
        try {
            guardar(nuevaPersona);
            System.out.println("Persona registrada con éxito con ID: " + nuevaPersona.getId());
        } catch (IllegalArgumentException e) {
            System.err.println("Error al registrar: " + e.getMessage());
        }
    }

    public void modificarPersona(UUID idOriginal, Persona datosNuevos) {
        Persona existente = buscarPorId(idOriginal).orElse(null);
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
            actualizar(idOriginal, existente);
            System.out.println("Persona actualizada con éxito.");
        } catch (IllegalArgumentException e) {
            System.err.println("Error al modificar: " + e.getMessage());
        }
    }

    public void agregarMedioDeContactoAPersona(UUID idPersona, MedioDeContacto nuevoMedio) {
        Persona persona = buscarPorId(idPersona).orElse(null);
        if (persona != null) {
            try {
                persona.agregarMedioDeContacto(nuevoMedio);
                actualizar(idPersona, persona);
                System.out.println("Medio de contacto agregado exitosamente a la persona: " + idPersona);
            } catch (IllegalStateException e) {
                System.err.println("Error al agregar medio de contacto: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("No se pudo agregar el medio de contacto: Persona no encontrada.");
        }
    }

    public void eliminarMedioDeContactoAPersona(UUID idPersona, MedioDeContacto medioAEliminar) {
        Persona persona = buscarPorId(idPersona).orElse(null);
        if (persona != null) {
            try {
                persona.eliminarMedioDeContacto(medioAEliminar);
                actualizar(idPersona, persona);
                System.out.println("Medio de contacto eliminado exitosamente de la persona: " + idPersona);
            } catch (IllegalStateException e) {
                System.err.println("Error al eliminar medio de contacto: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("No se pudo eliminar el medio de contacto: Persona no encontrada.");
        }
    }
}