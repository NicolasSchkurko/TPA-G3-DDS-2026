package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para Necesidad.
 * RepositorioDeNecesidades actúa como fachada sobre esta interfaz (ver RepositorioDeEntidadesBeneficiarias).
 */
public interface RepositorioDeNecesidades extends JpaRepository<Necesidad, UUID> {

    default Necesidad guardar(Necesidad necesidad) {
        if (necesidad == null) {
            return null;
        }
        if (necesidad.getId() == null) {
            necesidad.setId(UUID.randomUUID());
        }
        if (findById(necesidad.getId()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un Necesidad con el ID: " + necesidad.getId());
        }
        return save(necesidad);
    }

    default List<Necesidad> obtenerTodos() { return findAll(); }

    default Optional<Necesidad> buscarPorId(UUID id) { return findById(id); }

    default void actualizar(UUID idOriginal, Necesidad necesidadActualizada) {
        if (existsById(idOriginal)) {
            save(necesidadActualizada);
        } else {
            throw new IllegalArgumentException("No se encontró el Necesidad a actualizar.");
        }
    }

    default void eliminarPorId(UUID id) { deleteById(id); }

    default Necesidad modificarNecesidad(UUID idOriginal, Necesidad datosNuevos) {
        Necesidad existente = findById(idOriginal)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad con ID: " + idOriginal));

        if (!existente.getClass().equals(datosNuevos.getClass())) {
            throw new IllegalArgumentException(
                "No se puede cambiar el tipo de una necesidad existente (" +
                existente.getClass().getSimpleName() + " -> " +
                datosNuevos.getClass().getSimpleName() + ")");
        }

        existente.setCantidadObjetivo(datosNuevos.getCantidadObjetivo());
        existente.setDescripcion(datosNuevos.getDescripcion());
        existente.setSubcategoria(datosNuevos.getSubcategoria());

        if (existente instanceof NecesidadRecurrente existenteRecurrente) {
            existenteRecurrente.setPlazoEnDias(((NecesidadRecurrente) datosNuevos).getPlazoEnDias());
        }

        save(existente);
        return existente;
    }

}
