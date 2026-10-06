package ar.edu.utn.frba.ddsi.incentivos.services;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MetricaDonacionesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RegistroMensualDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Métricas agregadas sobre las donaciones de un donante.
 *
 * <p>Todo se calcula contra los impactos de donación que este servicio tiene copiados, no
 * contra {@code donaciones-service}: preguntar por cada request dejaría las métricas
 * inconsistentes con el progreso y ataría la disponibilidad de este endpoint a la del
 * otro servicio.
 */
@Service
public class MetricasService {
    private final RepositorioDonaciones repositorioDonaciones;

    public MetricasService(RepositorioDonaciones repositorioDonaciones) {
        this.repositorioDonaciones = repositorioDonaciones;
    }

    /**
     * La actividad de un donante agrupada por mes, más sus totales históricos.
     *
     * <p>Los meses sin donaciones no aparecen: la respuesta tiene una entrada por cada mes
     * en el que el donante donó, y el gráfico la arma con eso.
     */
    public ActividadDTO obtenerEvolucionHistorica(UUID idUsuario) {
        List<ImpactoDonacion> donaciones =
                repositorioDonaciones.findByIdUsuarioOrderByFechaEntregaAsc(idUsuario);
        Map<YearMonth, List<ImpactoDonacion>> porPeriodo = donaciones.stream()
                .collect(groupingBy(
                        donacion -> YearMonth.from(donacion.getFechaEntrega()),
                        TreeMap::new,
                        toList()));
        List<RegistroMensualDTO> registros = porPeriodo.entrySet().stream()
                .map(entry -> new RegistroMensualDTO(
                        entry.getKey(),
                        (long) entry.getValue().size(),
                        entry.getValue().stream()
                                .map(ImpactoDonacion::getEntidadBeneficiaria)
                                .filter(entidad -> entidad != null && !entidad.isBlank())
                                .distinct()
                                .count()
                ))
                .toList();

        return new ActividadDTO(
                registros,
                (long) donaciones.size(),
                donaciones.stream()
                        .map(ImpactoDonacion::getEntidadBeneficiaria)
                        .filter(entidad -> entidad != null && !entidad.isBlank())
                        .distinct()
                        .count()
        );
    }

    /**
     * Las donaciones del donante en un rango de fechas, con el total y las entidades
     * receptoras.
     *
     * <p>Devuelve {@code Optional} y no lanza si no hay donaciones: que un donante no
     * haya donado en el período pedido es una respuesta válida, no un error.
     *
     * <p>El rango es inclusivo en {@code hasta}: se hace {@code hasta + 1 día} a las
     * 00:00, porque las donaciones guardan fecha y hora y comparar contra
     * {@code hasta} a las 00:00 excluiría todo lo que se donó durante ese día.
     */
    public Optional<MetricaDonacionesDTO> obtenerMetrica(
            UUID idUsuario,
            LocalDate desde,
            LocalDate hasta
    ) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException(
                    "La fecha hasta no puede ser anterior a la fecha desde");
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        return repositorioDonaciones
                .obtenerResumenMetrica(idUsuario, inicio, fin)
                .map(resumen -> new MetricaDonacionesDTO(
                        (UUID) resumen[0],
                        ((Number) resumen[1]).longValue(),
                        ((Number) resumen[2]).longValue(),
                        (LocalDateTime) resumen[3],
                        (LocalDateTime) resumen[4],
                        repositorioDonaciones.obtenerEntidadesBeneficiarias(
                                idUsuario, inicio, fin)
                ));
    }
}
