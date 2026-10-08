package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resumen agregado de las donaciones de un donante en un período. Es la fila que devuelve
 * {@code obtenerResumenMetrica}: proyección con constructor para no mapear {@code Object[]}.
 */
public record ResumenMetricaDTO(
        UUID idUsuario,
        Long cantidadDonaciones,
        Long cantidadBienes,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin) {
}
