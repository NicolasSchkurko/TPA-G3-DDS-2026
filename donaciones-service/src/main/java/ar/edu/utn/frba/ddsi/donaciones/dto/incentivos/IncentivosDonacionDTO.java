package ar.edu.utn.frba.ddsi.donaciones.dto.incentivos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload del reporte de donación asignada a incentivos
 * ({@code PATCH /api/perfiles/donacion/{idUsuario}}).
 *
 * <p>El contrato lo define el lado receptor: incentivos exige {@code idDonacion}
 * (clave de idempotencia) y {@code fechaEntrega} como {@code LocalDateTime}
 * ({@code YearMonth.from(...)} para sus métricas mensuales). Un {@code LocalDate}
 * serializa como "2026-10-07" y Jackson del otro lado no lo parsea.
 */
@Getter
@Setter
public class IncentivosDonacionDTO {
    /** Id de la donación local. Incentivos lo exige: sin él responde 400. */
    private UUID idDonacion;
    private LocalDateTime fechaEntrega;
    private Integer cantidadBienes;
    private String subCategoria;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;
}