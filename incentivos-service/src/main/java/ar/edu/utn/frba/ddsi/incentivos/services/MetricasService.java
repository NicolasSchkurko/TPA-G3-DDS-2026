package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MetricaDonacionesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RegistroMensualDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
public class MetricasService {
    private final RepositorioDonaciones repositorioDonaciones;

    public MetricasService(RepositorioDonaciones repositorioDonaciones) {
        this.repositorioDonaciones = repositorioDonaciones;
    }

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
