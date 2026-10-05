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
     * La evolución mensual de un donante, ya agregada (punto 22).
     *
     * <p>Antes esto se armaba en Java: se traían <em>todas</em> las donaciones del donante y
     * después se agrupaban por mes con un {@code groupingBy}. Con un donante que donó 500
     * veces, el endpoint traía 500 filas enteras para devolver cinco números.
     *
     * <p>Acá la base devuelve una fila por mes, que es lo que el gráfico realmente necesita.
     * El filtro sigue siendo sargable porque la función está en el {@code GROUP BY} y no en
     * un {@code WHERE}: agrupar por mes es inevitable, y hacerlo sobre una columna indexada
     * con la función proyectada no impide que el índice se use para el {@code WHERE} de la
     * consulta de la otra mitad.
     *
     * <p>El {@code CASE} del {@code COUNT DISTINCT} no es adorno: el código anterior en Java
     * filtraba las entidades nulas y vacías antes de contarlas, y un {@code COUNT(DISTINCT
     * columna)} a secas cuenta la cadena vacía como una entidad distinta más.
     *
     * <p>El {@code TRIM} hace que {@code "Fundacion"} y {@code "Fundacion "} cuenten como la
     * misma entidad. El código anterior las contaba como dos, así que esto corrige un
     * sobreconteo que solo se notaba con datos mal cargados.
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
     * Los totales históricos del donante, ya agregados (punto 22).
     *
     * <p>Con la misma lógica de filtrado y {@code TRIM} que
     * {@link #obtenerEvolucionMensual}: si las dos mitades de la respuesta usan reglas
     * distintas de qué es una entidad distinta, el gráfico muestra un total que no cuadra
     * con la suma de los meses.
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
