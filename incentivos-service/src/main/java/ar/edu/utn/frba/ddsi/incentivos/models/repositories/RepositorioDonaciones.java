package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

}
