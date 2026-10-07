package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas sobre las donaciones, fuente de la que se calcula todo el progreso. Las copia
 * {@code donaciones-service} cuando una donación se entrega.
 */
@Repository
public interface RepositorioDonaciones
        extends JpaRepository<ImpactoDonacion, UUID> {

    List<ImpactoDonacion> findByIdUsuarioOrderByFechaEntregaAsc(UUID idUsuario);

    List<ImpactoDonacion> findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
            UUID idUsuario,
            UUID idMision
    );

    @Query("""
            SELECT d.idUsuario, COUNT(d), COALESCE(SUM(d.cantidadBienes), 0),
                   MIN(d.fechaEntrega), MAX(d.fechaEntrega)
            FROM ImpactoDonacion d
            WHERE d.idUsuario = :idUsuario
              AND d.fechaEntrega >= :desde
              AND d.fechaEntrega < :hasta
            GROUP BY d.idUsuario
            """)
    Optional<Object[]> obtenerResumenMetrica(
            @Param("idUsuario") UUID idUsuario,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
            SELECT DISTINCT d.entidadBeneficiaria
            FROM ImpactoDonacion d
            WHERE d.idUsuario = :idUsuario
              AND d.fechaEntrega >= :desde
              AND d.fechaEntrega < :hasta
              AND d.entidadBeneficiaria IS NOT NULL
              AND d.entidadBeneficiaria <> ''
            ORDER BY d.entidadBeneficiaria
            """)
    List<String> obtenerEntidadesBeneficiarias(
            @Param("idUsuario") UUID idUsuario,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    /**
     * La evolución mensual de un donante, agregada en la base: una fila por mes. El
     * {@code TRIM} y el {@code CASE} evitan contar la cadena vacía o con espacios como otra
     * entidad.
     *
     * @return una fila por mes con {@code [año, mes, cantidad, entidades distintas]}
     */
    @Query("""
            SELECT YEAR(d.fechaEntrega), MONTH(d.fechaEntrega), COUNT(d),
                   COUNT(DISTINCT CASE WHEN TRIM(d.entidadBeneficiaria) <> ''
                                       THEN TRIM(d.entidadBeneficiaria) END)
            FROM ImpactoDonacion d
            WHERE d.idUsuario = :idUsuario
            GROUP BY YEAR(d.fechaEntrega), MONTH(d.fechaEntrega)
            ORDER BY YEAR(d.fechaEntrega), MONTH(d.fechaEntrega)
            """)
    List<Object[]> obtenerEvolucionMensual(@Param("idUsuario") UUID idUsuario);

    /**
     * Los totales históricos del donante, con el mismo {@code TRIM} que
     * {@link #obtenerEvolucionMensual}.
     *
     * @return {@code [total de donaciones, entidades receptoras distintas]}
     */
    @Query("""
            SELECT COUNT(d),
                   COUNT(DISTINCT CASE WHEN TRIM(d.entidadBeneficiaria) <> ''
                                       THEN TRIM(d.entidadBeneficiaria) END)
            FROM ImpactoDonacion d
            WHERE d.idUsuario = :idUsuario
            """)
    Object[] obtenerTotalesDonaciones(@Param("idUsuario") UUID idUsuario);

}
