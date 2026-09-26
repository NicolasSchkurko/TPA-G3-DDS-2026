package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioMisiones extends JpaRepository<Mision, UUID> {

    // Buscar una misión específica por su nombre exacto
    Optional<Mision> findByNombreMision(String nombreMision);

    // Buscar todas las misiones creadas por un administrador en particular
    List<Mision> findByIdAdmin(UUID idAdmin);

    // Buscar misiones que contengan una palabra clave en su nombre (ignorando mayúsculas/minúsculas)
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

    default Mision actualizarMision(Mision misionActual, Mision misionModificada) {
        // Actualizar nombre de misión
        if (misionModificada.getNombreMision() != null) {
            misionActual.setNombreMision(misionModificada.getNombreMision());
        }

        // Actualizar descripción
        if (misionModificada.getDescripcion() != null) {
            misionActual.setDescripcion(misionModificada.getDescripcion());
        }

        // Actualizar insignia objetivo
        if (misionModificada.getInsigniaObjetivo() != null) {
            Insignia insigniaActualizada = new Insignia(
                misionModificada.getInsigniaObjetivo().getNombre(),
                misionModificada.getDescripcion() != null ? misionModificada.getDescripcion() : misionActual.getDescripcion()
            );
            misionActual.setInsigniaObjetivo(insigniaActualizada);
        }

        // Actualizar regla de progreso (constancia y operación)
        if (misionModificada.getReglaDeProgreso() != null) {
            misionActual.setReglaDeProgreso(misionModificada.getReglaDeProgreso());
        }

        return save(misionActual);
    }
}