package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MetricaDonacionesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RegistroMensualDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
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
     *
     * <p><b>El agrupado lo hace la base, no Java</b> (punto 22). Antes se traían todas las
     * donaciones del donante y se agrupaban acá con un {@code groupingBy}, así que un donante
     * con 500 donaciones cargaba 500 filas enteras para devolver cinco números. Ahora la
     * consulta devuelve una fila por mes.
     *
     * <p>Son dos consultas y no una porque son dos preguntas distintas con dos
     * agregaciones distintas: el desglose por mes y los totales históricos. Meter el total en
     * la misma consulta que el {@code GROUP BY} por mes devolvería un total por mes, no el
     * total de la historia.
     */
    public ActividadDTO obtenerEvolucionHistorica(UUID idUsuario) {
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
     * Saca un número de la fila de la agregación.
     *
     * <p>El cast es a {@code Number} y no a {@code Long} porque el tipo exacto que devuelve
     * el driver depende de la función de agregación y de la base: un {@code COUNT} puede
     * venir como {@code Long} o como {@code BigInteger} según el caso, y pedir
     * {@code (Long) fila[0]} revienta con un {@code ClassCastException} en runtime, justo
     * con la consulta que uno quiere que funcione.
     */
    private static long numero(Object valor) {
        return ((Number) valor).longValue();
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
