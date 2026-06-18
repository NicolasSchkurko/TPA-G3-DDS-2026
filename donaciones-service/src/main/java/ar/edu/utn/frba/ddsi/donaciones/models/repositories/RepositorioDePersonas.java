package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioDePersonas {

    // Eliminamos el patrón Singleton estático para usar inyección de dependencias de Spring
    private final List<PersonaDonante> personas = new ArrayList<>();

    public List<PersonaDonante> findAll() {
        return new ArrayList<>(personas);
    }

    public Optional<PersonaDonante> findById(UUID id) {
        return personas.stream()
                       .filter(p -> p.getId().equals(id))
                       .findFirst();
    }

    public PersonaDonante save(PersonaDonante persona) {
        // Al igual que en las otras entidades, borra si existe y lo vuelve a agregar actualizado
        deleteById(persona.getId());
        personas.add(persona);
        return persona;
    }

    public void deleteById(UUID id) {
        personas.removeIf(p -> p.getId().equals(id));
    }

    // Adaptamos tu método de búsqueda por nombre a la nueva estructura
    public Optional<PersonaDonante> findByNombreCompleto(String nombreBuscado) {
        if (nombreBuscado == null || nombreBuscado.trim().isEmpty()) {
            return Optional.empty();
        }

        String busqueda = nombreBuscado.trim();
        return personas.stream()
                       .filter(p -> {
                           try {
                               String nombrePersona = p.darNombre();
                               return nombrePersona != null && nombrePersona.trim().equalsIgnoreCase(busqueda);
                           } catch (Exception e) {
                               return false;
                           }
                       })
                       .findFirst();
    }
}
