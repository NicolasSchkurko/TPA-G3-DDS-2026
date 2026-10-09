package ar.edu.utn.frba.ddsi.donaciones.dto.incentivos;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload del reporte de impacto de una donación a incentivos
 * ({@code PATCH /api/perfiles/donacion/{idUsuario}}).
 *
 * <p>El contrato lo define el lado receptor: incentivos exige {@code idDonacion}
 * (clave de idempotencia) y {@code fechaEntrega} como {@code LocalDateTime}
 * ({@code YearMonth.from(...)} para sus métricas mensuales). Un {@code LocalDate}
 * serializa como "2026-10-07" y Jackson del otro lado no lo parsea.
 *
 * <p>{@code estado} viaja en el vocabulario de incentivos (el de sus reglas de misión:
 * "ENTREGADA"), no con el nombre del enum local.
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

    /** Arma el payload a partir de la donación entregada. */
    public static IncentivosDonacionDTO desde(Donacion donacion) {
        IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
        dto.setIdDonacion(donacion.getId());
        dto.setFechaEntrega(donacion.getFechaEntrega() != null
                ? donacion.getFechaEntrega().atStartOfDay()
                : null);
        dto.setCantidadBienes(donacion.sumaCantidadBienes());
        dto.setSubCategoria(donacion.getSubcategoria().getNombre());
        dto.setCategoria(donacion.getSubcategoria().getCategoria().getNombre());
        dto.setEntidadBeneficiaria(donacion.getEntidad().getPersonaJuridica().getRazonSocial());
        // "ENTREGADA" y no "ENTREGADO": es el valor con el que están configuradas las reglas
        // de misión del catálogo de incentivos (InicializadorCategorias).
        dto.setEstado("ENTREGADA");
        return dto;
    }
}