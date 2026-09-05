package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para Necesidad.
 * RepositorioNecesidades actúa como fachada sobre esta interfaz (ver EntidadBeneficiariaJpaRepository).
 */
public interface NecesidadJpaRepository extends JpaRepository<Necesidad, UUID> {
}
