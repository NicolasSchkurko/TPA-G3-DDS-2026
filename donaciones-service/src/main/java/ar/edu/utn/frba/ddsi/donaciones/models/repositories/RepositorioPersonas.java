package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio en memoria para gestionar operaciones CRUD sobre objetos Persona.
 */
public class RepositorioPersonas {
    private List<Persona> personasEnMemoria;

    public RepositorioPersonas() {
        this.personasEnMemoria = new ArrayList<>();
    }

    // Create
    public void guardar(Persona persona) {
        if (persona != null && persona.getId() != null) {
            // Verificamos si ya existe para evitar duplicados por ID
            if (buscarPorId(persona.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una persona con el ID: " + persona.getId());
            }
            this.personasEnMemoria.add(persona);
        }
    }

    // Read
    public List<Persona> obtenerTodas() {
        return new ArrayList<>(this.personasEnMemoria); // Devolvemos una copia para proteger la lista original
    }

    public Optional<Persona> buscarPorId(UUID id) {
        return this.personasEnMemoria.stream()
                                     .filter(p -> p.getId().equals(id))
                                     .findFirst();
    }

    // Update
    public void actualizar(UUID idOriginal, Persona personaActualizada) {
        Optional<Persona> personaExistente = buscarPorId(idOriginal);
        if (personaExistente.isPresent()) {
            int index = this.personasEnMemoria.indexOf(personaExistente.get());
            this.personasEnMemoria.set(index, personaActualizada);
        } else {
            throw new IllegalArgumentException("No se encontró la persona a actualizar.");
        }
    }

    // Delete
    public void eliminarPorId(UUID id) {
        this.personasEnMemoria.removeIf(p -> p.getId().equals(id));
    }
}