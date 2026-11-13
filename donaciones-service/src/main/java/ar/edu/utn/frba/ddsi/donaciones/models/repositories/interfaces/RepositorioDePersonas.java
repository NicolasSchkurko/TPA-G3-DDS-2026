package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para Persona (incluye Humana y Juridica, mapeadas con herencia JOINED).
 * RepositorioDePersonas actúa como fachada sobre esta interfaz (ver RepositorioDeEntidadesBeneficiarias).
 */
public interface RepositorioDePersonas extends JpaRepository<Persona, UUID> {

    default void guardar(Persona persona) {
        if (persona != null && persona.getId() != null) {
            if (findById(persona.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una persona con el ID: " + persona.getId());
            }
            save(persona);
        }
    }

    default List<Persona> obtenerTodas() { return findAll(); }

    default Optional<Persona> buscarPorId(UUID id) { return findById(id); }

    default void actualizar(UUID idOriginal, Persona personaActualizada) {
        if (existsById(idOriginal)) {
            save(personaActualizada);
        } else {
            throw new IllegalArgumentException("No se encontró la persona a actualizar.");
        }
    }

    default void eliminarPorId(UUID id) { deleteById(id); }

    default void registrarPersona(Persona nuevaPersona) { guardar(nuevaPersona); }

    default void modificarPersona(UUID idOriginal, Persona datosNuevos) {
        Persona existente = findById(idOriginal)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con ID: " + idOriginal));

        existente.setMediosDeContacto(datosNuevos.getMediosDeContacto());

        if (existente instanceof Humana ph && datosNuevos instanceof Humana phNuevos) {
            ph.setNombre(phNuevos.getNombre());
            ph.setApellido(phNuevos.getApellido());
            ph.setEdad(phNuevos.getEdad());
            ph.setNumeroDeDocumento(phNuevos.getNumeroDeDocumento());
            ph.setGenero(phNuevos.getGenero());
        }

        if (existente instanceof Juridica pj && datosNuevos instanceof Juridica pjNuevos) {
            pj.setRazonSocial(pjNuevos.getRazonSocial());
            pj.setCuit(pjNuevos.getCuit());
            pj.setRubro(pjNuevos.getRubro());
            pj.setTipoJuridico(pjNuevos.getTipoJuridico());
            if (pjNuevos.getRepresentantes() != null) {
                if (pj.getRepresentantes() != null) {
                    pj.getRepresentantes().clear();
                    pj.getRepresentantes().addAll(pjNuevos.getRepresentantes());
                } else {
                    pj.setRepresentantes(new ArrayList<>(pjNuevos.getRepresentantes()));
                }
            }
        }

        save(existente);
    }

    default void agregarMedioDeContactoAPersona(UUID idPersona, MedioDeContacto nuevoMedio) {
        Persona persona = findById(idPersona)
            .orElseThrow(() -> new IllegalArgumentException("No se pudo agregar el medio de contacto: Persona no encontrada."));
        persona.agregarMedioDeContacto(nuevoMedio);
        save(persona);
    }

    default void eliminarMedioDeContactoAPersona(UUID idPersona, MedioDeContacto medioAEliminar) {
        Persona persona = findById(idPersona)
            .orElseThrow(() -> new IllegalArgumentException("No se pudo eliminar el medio de contacto: Persona no encontrada."));
        persona.eliminarMedioDeContacto(medioAEliminar);
        save(persona);
    }

}
