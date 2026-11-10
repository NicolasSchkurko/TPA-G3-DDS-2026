package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.EntidadBeneficiariaJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre EntidadBeneficiariaJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria,
 * para no tener que tocar los Gestores/Services que ya la usan.
 */
@Repository
public class RepositorioEntidadesBeneficiarias {

    private final EntidadBeneficiariaJpaRepository jpaRepository;

    public RepositorioEntidadesBeneficiarias(EntidadBeneficiariaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void guardar(EntidadBeneficiaria entidad) {
        if (entidad != null && entidad.getId() != null) {
            if (buscarPorId(entidad.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una entidad con el ID: " + entidad.getId());
            }
            jpaRepository.save(entidad);
        }
    }

    public List<EntidadBeneficiaria> obtenerTodas() {
        return jpaRepository.findAll();
    }

    public Page<EntidadBeneficiaria> obtenerTodasActivas(Pageable pageable) {
        return jpaRepository.findByActivaTrue(pageable);
    }

    public Optional<EntidadBeneficiaria> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    /** Entidad activa vinculada a la cuenta de Keycloak indicada. */
    public Optional<EntidadBeneficiaria> buscarPorKeycloakSub(String keycloakSub) {
        if (keycloakSub == null || keycloakSub.isBlank()) {
            return Optional.empty();
        }
        return jpaRepository.findByKeycloakSubAndActivaTrue(keycloakSub.trim());
    }

    /** Entidad activa cuyo responsable tiene ese mail como medio de contacto. */
    public Optional<EntidadBeneficiaria> buscarPorEmailContacto(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        List<EntidadBeneficiaria> resultados = jpaRepository.buscarPorEmailContacto(email.trim().toLowerCase());
        return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
    }

    public void actualizar(UUID idOriginal, EntidadBeneficiaria entidadActualizada) {
        if (jpaRepository.existsById(idOriginal)) {
            jpaRepository.save(entidadActualizada);
        } else {
            throw new IllegalArgumentException("No se encontró la entidad a actualizar.");
        }
    }

    public void flush() {
        jpaRepository.flush();
    }

    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }

    // --- Antes vivían en GestorEntidadesBeneficiarias: son manejo de dominio de la propia
    // entidad, se movieron acá para no mantener un gestor que solo delegaba en el repositorio. ---

    public EntidadBeneficiaria modificarEntidad(UUID idOriginal, EntidadBeneficiaria datosNuevos) {
        EntidadBeneficiaria existente = buscarPorId(idOriginal).orElse(null);
        if (existente == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + idOriginal);
        }
        existente.setDireccion(datosNuevos.getDireccion());

        try {
            actualizar(idOriginal, existente);
            System.out.println("Entidad beneficiaria actualizada con éxito.");
        } catch (IllegalArgumentException e) {
            System.err.println("Error al modificar entidad: " + e.getMessage());
        }

        return existente;
    }

    public void agregarNecesidadAEntidad(UUID idEntidad, Necesidad nuevaNecesidad) {
        EntidadBeneficiaria entidad = buscarPorId(idEntidad).orElse(null);
        if (entidad != null) {
            entidad.agregarNecesidad(nuevaNecesidad);
            actualizar(idEntidad, entidad);
            System.out.println("Necesidad agregada a la entidad: " + idEntidad);
        } else {
            throw new IllegalArgumentException("No se pudo agregar necesidad: Entidad no encontrada.");
        }
    }

    public void eliminarNecesidadDeEntidad(UUID idEntidad, UUID idNecesidad) {
        EntidadBeneficiaria entidad = buscarPorId(idEntidad).orElse(null);
        if (entidad == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad);
        }

        Necesidad necesidad = entidad.buscarNecesidadPorId(idNecesidad)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la necesidad con ID: " + idNecesidad));

        entidad.eliminarNecesidad(necesidad);
        actualizar(idEntidad, entidad);
        System.out.println("Necesidad desvinculada de la entidad con éxito.");
    }
}
