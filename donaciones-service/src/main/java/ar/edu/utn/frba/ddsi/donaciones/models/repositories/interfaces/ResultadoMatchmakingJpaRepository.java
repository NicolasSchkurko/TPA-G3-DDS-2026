package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para ResultadoMatchmaking.
 * No se usa directamente desde los Gestores/Services: RepositorioDeResultadosMatchmaking actúa
 * como fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface ResultadoMatchmakingJpaRepository extends JpaRepository<ResultadoMatchmaking, UUID> {
    Optional<ResultadoMatchmaking> findByDonacion_Id(UUID donacionId);
}
