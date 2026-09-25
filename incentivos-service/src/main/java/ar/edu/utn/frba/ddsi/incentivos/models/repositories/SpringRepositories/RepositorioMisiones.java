package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioMisiones extends JpaRepository<Mision, UUID> {

    // NOTA: JpaRepository ya incluye por defecto y listos para usar en tus Gestores:
    // - findById(UUID id)
    // - findAll()
    // - findAllById(Iterable<UUID> ids)
    // - save(Mision mision)
    // - delete(Mision mision)
    // - deleteById(UUID id)

    // Búsquedas personalizadas (Spring Data genera el SQL automáticamente al leer el nombre):

    // Buscar una misión específica por su nombre exacto
    Optional<Mision> findByNombreMision(String nombreMision);

    // Buscar todas las misiones creadas por un administrador en particular
    List<Mision> findByIdAdmin(UUID idAdmin);

    // Buscar misiones que contengan una palabra clave en su nombre (ignorando mayúsculas/minúsculas)
    // Muy útil si quieres hacer un buscador en el frontend
    List<Mision> findByNombreMisionContainingIgnoreCase(String keyword);

    default List<Mision> conseguirMisiones(List<UUID> idMisiones) {
        if (idMisiones == null || idMisiones.isEmpty()) {
            return List.of();
        }

        List<Mision> misiones = findAllById(idMisiones);
        if (misiones.size() != idMisiones.size()) {
            throw new IllegalArgumentException("Una o más misiones solicitadas no existen");
        }
        return misiones;
    }

    default Mision obtenerPorId(UUID id) {
        return findById(id).orElse(null);
    }

    default Mision eliminarMision(UUID idMision) {
        Mision m = findById(idMision).orElse(null);
        if (m != null) {
            delete(m);
        }
        return m;
    }
}