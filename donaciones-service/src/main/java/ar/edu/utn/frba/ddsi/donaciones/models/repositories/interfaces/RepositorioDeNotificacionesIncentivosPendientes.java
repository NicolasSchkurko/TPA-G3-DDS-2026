package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.incentivos.NotificacionIncentivosPendiente;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a la outbox durable de notificaciones pendientes hacia incentivos. */
public interface RepositorioDeNotificacionesIncentivosPendientes
    extends JpaRepository<NotificacionIncentivosPendiente, UUID> {

    /** Filas disponibles, sin lease vigente, ordenadas por creación. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificacionIncentivosPendiente n "
        + "WHERE n.proximoIntento <= :ahora "
        + "AND (n.leaseHasta IS NULL OR n.leaseHasta <= :ahora) "
        + "ORDER BY n.fechaCreacion, n.id")
    List<NotificacionIncentivosPendiente> buscarDisponibles(@Param("ahora") LocalDateTime ahora,
                                                              Pageable pageable);

    /** La misma fila, solo si sigue disponible y sin lease vigente. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificacionIncentivosPendiente n "
        + "WHERE n.id = :id "
        + "AND n.proximoIntento <= :ahora "
        + "AND (n.leaseHasta IS NULL OR n.leaseHasta <= :ahora)")
    Optional<NotificacionIncentivosPendiente> buscarPorIdDisponible(
        @Param("id") UUID id, @Param("ahora") LocalDateTime ahora);
}
