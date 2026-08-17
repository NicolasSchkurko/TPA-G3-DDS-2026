package ar.edu.utn.frba.ddsi.donaciones.models.repositories;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioAdministradores {
    //parece q no hay mas de 1 admin x ahora, esto queda x si acaso
    private final List<Administrador> administradores = new ArrayList<>();

    public List<Administrador> findAll() {
        return new ArrayList<>(administradores);
    }

    public Optional<Administrador> findById(UUID id) {
        return administradores.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    public Administrador save(Administrador persona) {
        // Al igual que en las otras entidades, borra si existe y lo vuelve a agregar actualizado
        deleteById(persona.getId());
        administradores.add(persona);
        return persona;
    }

    public void deleteById(UUID id) {
        administradores.removeIf(p -> p.getId().equals(id));
    }

    // Adaptamos tu método de búsqueda por nombre a la nueva estructura
    public Optional<Administrador> findByNombreCompleto(String nombreBuscado) {
        if (nombreBuscado == null || nombreBuscado.trim().isEmpty()) {
            return Optional.empty();
        }

        String busqueda = nombreBuscado.trim();
        return administradores.stream()
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
