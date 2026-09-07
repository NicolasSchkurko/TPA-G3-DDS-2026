package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.MetricaActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Repository
public interface RepositorioActividad
        extends JpaRepository<ActividadMensual, UUID> {

    Optional<ActividadMensual> findByPerfilAndPeriodo(
            Perfil perfil,
            YearMonth periodo
    );

    List<ActividadMensual> findByPerfilIdUsuarioOrderByPeriodoAsc(UUID idUsuario);

    Optional<ActividadMensual> findByPerfilIdUsuarioAndPeriodo(
            UUID idUsuario,
            YearMonth periodo
    );

    Optional<ActividadMensual> findTopByPerfilIdUsuarioAndPeriodoLessThanOrderByPeriodoDesc(
            UUID idUsuario,
            YearMonth periodo
    );

    @Query("SELECT COALESCE(SUM(a.cantidadDonaciones), 0) " +
           "FROM ActividadMensual a WHERE a.perfil.idUsuario = :idUsuario")
    Long totalDonaciones(@Param("idUsuario") UUID idUsuario);

    @Query("SELECT COUNT(DISTINCT organizacion) " +
           "FROM ActividadMensual a JOIN a.entidadesBeneficiadas organizacion " +
           "WHERE a.perfil.idUsuario = :idUsuario")
    Long totalOrganizaciones(@Param("idUsuario") UUID idUsuario);

    default List<MetricaActividad> obtenerMetricasDesdeActividad(UUID idUsuario) {
        List<ActividadMensual> actividades =
                findByPerfilIdUsuarioOrderByPeriodoAsc(idUsuario);

        return IntStream.range(0, Math.max(0, actividades.size() - 1))
                .mapToObj(indice -> {
                    ActividadMensual actual = actividades.get(indice);
                    ActividadMensual siguiente = actividades.get(indice + 1);
                    double variacion = actual.getCantidadDonaciones() == 0
                            ? 0.0
                            : (siguiente.getCantidadDonaciones()
                            - actual.getCantidadDonaciones()) * 100.0
                            / actual.getCantidadDonaciones();

                    return new MetricaActividad(
                            actual.getPeriodo(),
                            siguiente.getPeriodo(),
                            variacion
                    );
                })
                .toList();
    }

    default Optional<MetricaActividad> obtenerMetricaPeriodoActual(
            UUID idUsuario,
            YearMonth periodoActual
    ) {
        Optional<ActividadMensual> actividadActual =
                findByPerfilIdUsuarioAndPeriodo(idUsuario, periodoActual);
        Optional<ActividadMensual> actividadAnterior =
                findTopByPerfilIdUsuarioAndPeriodoLessThanOrderByPeriodoDesc(
                        idUsuario,
                        periodoActual
                );

        if (actividadActual.isEmpty() || actividadAnterior.isEmpty()) {
            return Optional.empty();
        }

        ActividadMensual actual = actividadActual.get();
        ActividadMensual anterior = actividadAnterior.get();
        double variacion = anterior.getCantidadDonaciones() == 0
                ? 0.0
                : (actual.getCantidadDonaciones()
                - anterior.getCantidadDonaciones()) * 100.0
                / anterior.getCantidadDonaciones();

        return Optional.of(new MetricaActividad(
                anterior.getPeriodo(),
                actual.getPeriodo(),
                variacion
        ));
    }
}
