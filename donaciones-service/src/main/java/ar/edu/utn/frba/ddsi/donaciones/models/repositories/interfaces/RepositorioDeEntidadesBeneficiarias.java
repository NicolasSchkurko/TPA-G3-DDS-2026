package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para EntidadBeneficiaria.
 * No se usa directamente desde los Gestores/Services: RepositorioDeEntidadesBeneficiarias
 * actúa como fachada sobre esta interfaz, manteniendo los métodos públicos que ya existían
 * cuando el repositorio era en memoria (guardar, obtenerTodas, buscarPorId, actualizar, eliminarPorId).
 */
public interface RepositorioDeEntidadesBeneficiarias extends JpaRepository<EntidadBeneficiaria, UUID> {
    Page<EntidadBeneficiaria> findByActivaTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"necesidades", "necesidades.subcategoria"})
    List<EntidadBeneficiaria> findByActivaTrue();

    default void guardar(EntidadBeneficiaria entidad) {
        if (entidad != null && entidad.getId() != null) {
            if (findById(entidad.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una entidad con el ID: " + entidad.getId());
            }
            save(entidad);
        }
    }

    default List<EntidadBeneficiaria> obtenerTodas() { return findAll(); }

    default List<EntidadBeneficiaria> obtenerTodasActivas() { return findByActivaTrue(); }

    default Page<EntidadBeneficiaria> obtenerTodasActivas(Pageable pageable) {
        return findByActivaTrue(pageable);
    }

    default Optional<EntidadBeneficiaria> buscarPorId(UUID id) { return findById(id); }

    default void actualizar(UUID idOriginal, EntidadBeneficiaria entidadActualizada) {
        if (existsById(idOriginal)) {
            save(entidadActualizada);
        } else {
            throw new IllegalArgumentException("No se encontró la entidad a actualizar.");
        }
    }

    default void eliminarPorId(UUID id) { deleteById(id); }

    default EntidadBeneficiaria modificarEntidad(UUID idOriginal, EntidadBeneficiaria datosNuevos) {
        EntidadBeneficiaria existente = findById(idOriginal)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad con ID: " + idOriginal));
        existente.setDireccion(datosNuevos.getDireccion());
        save(existente);
        return existente;
    }

    default void agregarNecesidadAEntidad(UUID idEntidad, Necesidad nuevaNecesidad) {
        EntidadBeneficiaria entidad = findById(idEntidad)
            .orElseThrow(() -> new IllegalArgumentException("No se pudo agregar necesidad: Entidad no encontrada."));
        entidad.agregarNecesidad(nuevaNecesidad);
        save(entidad);
    }

    default void eliminarNecesidadDeEntidad(UUID idEntidad, UUID idNecesidad) {
        EntidadBeneficiaria entidad = findById(idEntidad)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad));
        Necesidad necesidad = entidad.buscarNecesidadPorId(idNecesidad)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la necesidad con ID: " + idNecesidad));
        entidad.eliminarNecesidad(necesidad);
        save(entidad);
    }

}
