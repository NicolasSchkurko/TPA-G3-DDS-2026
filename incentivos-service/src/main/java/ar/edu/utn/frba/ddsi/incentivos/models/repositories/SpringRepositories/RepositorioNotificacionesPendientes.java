package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Notificaciones.NotificacionPendiente;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a la outbox durable de notificaciones pendientes de publicar en RabbitMQ. */
@Repository
public interface RepositorioNotificacionesPendientes
        extends JpaRepository<NotificacionPendiente, UUID> {

    /**
     * Trae las notificaciones disponibles ordenadas por creación. El {@code Pageable} acota
     * el bloqueo pesimista a la fila que se va a reclamar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificacionPendiente n "
            + "WHERE n.descartada = false "
            + "AND n.proximoIntento <= :ahora "
            + "AND (n.leaseHasta IS NULL OR n.leaseHasta <= :ahora) "
            + "ORDER BY n.fechaCreacion, n.id")
    List<NotificacionPendiente> buscarSiguienteParaEnviar(
            @Param("ahora") LocalDateTime ahora,
            Pageable pageable
    );
}
