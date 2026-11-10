package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.logistica.EventoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistencia JPA de {@link EventoProcesado}. La tabla de deduplicación de eventos de
 * logística: clave primaria {@code id_evento} ({@code Long}).
 */
public interface RepositorioDeEventosProcesados extends JpaRepository<EventoProcesado, Long> {

    default boolean existe(long idEvento) { return existsById(idEvento); }

    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO eventos_logistica_procesados (id_evento, fecha_procesamiento) "
        + "VALUES (:idEvento, :fecha)", nativeQuery = true)
    void insertarEventoProcesado(@Param("idEvento") long idEvento,
                                 @Param("fecha") LocalDateTime fecha);

    default void reclamar(long idEvento) {
        insertarEventoProcesado(idEvento, LocalDateTime.now());
    }

}
