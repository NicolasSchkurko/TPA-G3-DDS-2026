package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Impacto de una donación, reportado por {@code donaciones-service}. {@code fechaEntrega} es
 * obligatoria porque las métricas y la constancia la usan.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImpactoDonacionDTO {

    /**
     * Id de la donación en el servicio de origen: la primary key local que hace idempotente
     * el endpoint. Obligatorio para poder deduplicar.
     */
    @NotNull(message = "La donación requiere su id de origen")
    private UUID idDonacion;

    @NotNull(message = "La donación requiere una fecha de entrega")
    private LocalDateTime fechaEntrega;

    @NotNull(message = "La donación requiere la cantidad de bienes")
    @PositiveOrZero(message = "La cantidad de bienes no puede ser negativa")
    private Integer cantidadBienes;

    private String subCategoria;
    @NotBlank(message = "La donación requiere una categoría")
    private String categoria;

    @NotBlank(message = "La donación requiere una entidad beneficiaria")
    private String entidadBeneficiaria;

    @NotBlank(message = "La donación requiere un estado")
    private String estado;
}
