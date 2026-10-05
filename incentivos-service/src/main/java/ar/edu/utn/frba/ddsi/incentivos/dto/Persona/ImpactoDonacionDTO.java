package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Impacto de una donación, reportado por donating-service.
 *
 * <p>{@code fechaEntrega} es obligatoria a propósito: las métricas mensuales hacen
 * {@code YearMonth.from(fechaEntrega)} y las reglas de constancia comparan contra
 * ella, así que una fecha nula reventaba con un NullPointerException (500) en lugar
 * de rechazar el pedido con un 400.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImpactoDonacionDTO {

    @NotNull(message = "La donación requiere una fecha de entrega")
    private LocalDateTime fechaEntrega;

    @NotNull(message = "La donación requiere la cantidad de bienes")
    @PositiveOrZero(message = "La cantidad de bienes no puede ser negativa")
    private Integer cantidadBienes;

    private String subCategoria;
    private String categoria;

    @NotBlank(message = "La donación requiere una entidad beneficiaria")
    private String entidadBeneficiaria;

    @NotBlank(message = "La donación requiere un estado")
    private String estado;
}