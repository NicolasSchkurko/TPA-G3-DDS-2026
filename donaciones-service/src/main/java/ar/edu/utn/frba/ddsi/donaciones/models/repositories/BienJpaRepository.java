package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para Bien (jerarquía JOINED: BienConEstado/BienPerecedero).
 * No se usa directamente desde los Gestores/Services: RepositorioBienes actúa como fachada.
 */
public interface BienJpaRepository extends JpaRepository<Bien, UUID> {
}
