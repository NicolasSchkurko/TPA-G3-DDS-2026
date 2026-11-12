package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioNotificaciones extends JpaRepository<Notificacion, UUID> {
    @Query("select n from Notificacion n where :idSolicitud is not null "
            + "and n.idSolicitud = :idSolicitud")
    Optional<Notificacion> findByIdSolicitud(@Param("idSolicitud") String idSolicitud);

    @EntityGraph(attributePaths = "mensaje")
    Page<Notificacion> findByEstado(EstadoNotificacion estado, Pageable pageable);

    @Query(value = "select n from Notificacion n where (:estado is null or n.estado = :estado)",
            countQuery = "select count(n) from Notificacion n where (:estado is null or n.estado = :estado)")
    @EntityGraph(attributePaths = "mensaje")
    Page<Notificacion> buscar(@Param("estado") EstadoNotificacion estado, Pageable pageable);

    @Query(value = "select n from Notificacion n where "
            + "lower(n.direccionDeContacto) = lower(:destinatario) "
            + "and (:estado is null or n.estado = :estado)",
            countQuery = "select count(n) from Notificacion n where "
            + "lower(n.direccionDeContacto) = lower(:destinatario) "
            + "and (:estado is null or n.estado = :estado)")
    @EntityGraph(attributePaths = "mensaje")
    Page<Notificacion> buscarPorDestinatario(@Param("destinatario") String destinatario,
                                             @Param("estado") EstadoNotificacion estado,
                                             Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "mensaje")
    Page<Notificacion> findAll(Pageable pageable);

    /**
     * Lee la notificación con bloqueo pesimista para el despacho de un aviso: dos entregas
     * concurrentes del mismo aviso se serializan, la segunda ve el estado que dejó la primera y
     * no vuelve a enviar. Requiere una transacción activa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notificacion n where n.id = :id")
    Optional<Notificacion> findByIdParaEnvio(@Param("id") UUID id);

    /** Cierra un envío reclamado: solo pisa si sigue ENVIANDO con el mismo claim. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Notificacion n set n.estado = :enviada, n.fechaEnvio = :ahora, "
            + "n.fechaIntento = null, n.claimId = null "
            + "where n.id = :id and n.estado = :enviando and n.claimId = :claimId")
    int marcarEnviadaSiEnviando(@Param("id") UUID id,
                                @Param("claimId") UUID claimId,
                                @Param("ahora") LocalDateTime ahora,
                                @Param("enviada") EstadoNotificacion enviada,
                                @Param("enviando") EstadoNotificacion enviando);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Notificacion n set n.estado = :fallida, n.fechaIntento = null, n.claimId = null "
            + "where n.id = :id and n.estado = :enviando and n.claimId = :claimId")
    int marcarFallidaSiEnviando(@Param("id") UUID id,
                                @Param("claimId") UUID claimId,
                                @Param("fallida") EstadoNotificacion fallida,
                                @Param("enviando") EstadoNotificacion enviando);

    /** Devuelve a PENDIENTE los envíos que quedaron colgados (proceso muerto antes de cerrar). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Notificacion n set n.estado = :pendiente, n.fechaIntento = null, "
            + "n.claimId = null, n.intentos = coalesce(n.intentos, 0) + 1 "
            + "where n.estado = :enviando "
            + "and (n.fechaIntento is null or n.fechaIntento < :limite)")
    int liberarEnviosColgados(@Param("pendiente") EstadoNotificacion pendiente,
                              @Param("enviando") EstadoNotificacion enviando,
                              @Param("limite") LocalDateTime limite);

    /**
     * Candidatas al reintento automático, en lotes y sin el Mensaje: la proyección trae solo lo
     * que el scheduler necesita para reclamar y republicar el aviso.
     */
    @Query("select new ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.NotificacionReintentable("
            + "n.id, n.intentos, n.tipoMedioDeContacto, n.direccionDeContacto) "
            + "from Notificacion n where n.estado = :estado and n.fechaCreacion < :limite "
            + "and coalesce(n.intentos, 0) < :maxIntentos order by n.fechaCreacion")
    List<NotificacionReintentable> findParaReintentar(@Param("estado") EstadoNotificacion estado,
                                                      @Param("limite") LocalDateTime limite,
                                                      @Param("maxIntentos") int maxIntentos,
                                                      Pageable pageable);

    /**
     * Reclama una notificación para un intento automático con un compare-and-set sobre {@code intentos}:
     * incrementa solo si el valor sigue siendo {@code esperados} (el leído) y no agotó el tope. Devuelve 1
     * si esta instancia la tomó (y debe publicar) o 0 si otra réplica se adelantó o ya no corresponde.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Notificacion n set n.intentos = coalesce(n.intentos, 0) + 1 "
            + "where n.id = :id and n.estado = :estado "
            + "and coalesce(n.intentos, 0) = :esperados and coalesce(n.intentos, 0) < :maxIntentos")
    int reclamarParaReintento(@Param("id") UUID id,
                              @Param("estado") EstadoNotificacion estado,
                              @Param("esperados") int esperados,
                              @Param("maxIntentos") int maxIntentos);

    /** Devuelve el intento consumido cuando la publicación falló, para no agotar el tope sin haber publicado. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Notificacion n set n.intentos = coalesce(n.intentos, 0) - 1 "
            + "where n.id = :id and n.estado = :estado and coalesce(n.intentos, 0) > 0")
    int compensarReintentoFallido(@Param("id") UUID id, @Param("estado") EstadoNotificacion estado);
}
