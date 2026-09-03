package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;

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
}