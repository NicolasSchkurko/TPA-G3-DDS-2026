package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.PublicacionPendienteN8n;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a la outbox durable de publicaciones pendientes para n8n. */
@Repository
public interface RepositorioPublicacionesPendientes
        extends JpaRepository<PublicacionPendienteN8n, UUID> {

    /** Trae las publicaciones disponibles ordenadas por creación; el service toma la primera. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PublicacionPendienteN8n p "
            + "WHERE p.proximoIntento <= :ahora "
            + "AND (p.leaseHasta IS NULL OR p.leaseHasta <= :ahora) "
            + "ORDER BY p.fechaCreacion, p.id")
    List<PublicacionPendienteN8n> buscarSiguienteParaEnviar(
            @Param("ahora") LocalDateTime ahora
    );
}
