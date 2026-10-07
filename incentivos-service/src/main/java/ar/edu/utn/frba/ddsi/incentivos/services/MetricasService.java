package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MetricaDonacionesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RegistroMensualDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Métricas agregadas sobre las donaciones de un donante. Se calculan contra los impactos
 * copiados localmente, no contra {@code donaciones-service}.
 */
@Service
public class MetricasService {
    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioPerfiles repositorioPerfiles;

    public MetricasService(RepositorioDonaciones repositorioDonaciones,
                           RepositorioPerfiles repositorioPerfiles) {
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioPerfiles = repositorioPerfiles;
    }

    /**
     * La actividad de un donante agrupada por mes, más sus totales históricos. El agrupado lo
     * hace la base. Si el perfil no existe es 404.
     */
    public ActividadDTO obtenerEvolucionHistorica(UUID idUsuario) {
        if (!repositorioPerfiles.existsByIdUsuario(idUsuario)) {
            throw new InexistenteException();
        }

        List<RegistroMensualDTO> registros =
                repositorioDonaciones.obtenerEvolucionMensual(idUsuario).stream()
                        .map(fila -> new RegistroMensualDTO(
                                YearMonth.of((int) numero(fila[0]), (int) numero(fila[1])),
                                numero(fila[2]),
                                numero(fila[3])
                        ))
                        .toList();

        Object[] totales = repositorioDonaciones.obtenerTotalesDonaciones(idUsuario);

        return new ActividadDTO(
                registros,
                numero(totales[0]),
                numero(totales[1])
        );
    }

    /**
     * Saca un número de la fila de agregación. El cast es a {@code Number} porque el driver
     * puede devolver {@code Long} o {@code BigInteger}.
     */
    private static long numero(Object valor) {
        return ((Number) valor).longValue();
    }

    /**
     * Las donaciones del donante en un rango de fechas, con total y entidades. Devuelve
     * {@code Optional}: no donar en el período no es un error. El rango es inclusivo en
     * {@code hasta}.
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
