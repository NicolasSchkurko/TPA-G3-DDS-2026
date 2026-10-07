package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ResumenMetricaDTO;
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
 * Consultas sobre las donaciones, que es la fuente de la que se calcula todo el progreso.
 *
 * <p>Los impactos de donación los copia {@code donaciones-service} cuando una donación se
 * entrega. Los métodos que filtran por rango de fechas son los que usan las métricas y el
 * cálculo de constancia, que necesita los meses calendario.
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
            SELECT new ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ResumenMetricaDTO(
                   :idUsuario, COUNT(d), COALESCE(SUM(d.cantidadBienes), 0L),
                   MIN(d.fechaEntrega), MAX(d.fechaEntrega))
            FROM ImpactoDonacion d
            WHERE d.idUsuario = :idUsuario
              AND d.fechaEntrega >= :desde
              AND d.fechaEntrega <= :hasta
            """)
    Optional<ResumenMetricaDTO> obtenerResumenMetrica(
        @Param("idUsuario") UUID idUsuario,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta
    );

    @Query("""
            SELECT DISTINCT d.entidadBeneficiaria
            FROM ImpactoDonacion d
            WHERE d.idUsuario = :idUsuario
              AND d.fechaEntrega >= :desde
              AND d.fechaEntrega <= :hasta
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
     * @return una fila por mes con [año, mes, cantidad, entidades distintas]
     */
    @Query(value = """
            SELECT EXTRACT(YEAR FROM fecha_entrega) AS anio, 
                   EXTRACT(MONTH FROM fecha_entrega) AS mes, 
                   COUNT(*) AS cantidad,
                   COUNT(DISTINCT NULLIF(TRIM(entidad_beneficiaria), '')) AS entidades_distintas
            FROM impacto_donacion
            WHERE id_usuario = :idUsuario
            GROUP BY EXTRACT(YEAR FROM fecha_entrega), EXTRACT(MONTH FROM fecha_entrega)
            ORDER BY 1, 2
            """, nativeQuery = true)
    List<Object[]> obtenerEvolucionMensual(@Param("idUsuario") UUID idUsuario);

    /**
     * @return [total de donaciones, entidades receptoras distintas]
     */
    @Query(value = """
            SELECT COUNT(*) AS total_donaciones,
                   COUNT(DISTINCT NULLIF(TRIM(entidad_beneficiaria), '')) AS entidades_receptoras_distintas
            FROM impacto_donacion
            WHERE id_usuario = :idUsuario
            """, nativeQuery = true)
    Object[] obtenerTotalesDonaciones(@Param("idUsuario") UUID idUsuario);

}
