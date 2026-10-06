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
 * Impacto de una donación, reportado por donaciones-service.
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

    /**
     * Id de la DONACION en el servicio de origen. Se guarda tal cual: es la primary key de
     * la donación local y lo que hace idempotente el endpoint (punto 14).
     *
     * <p>Es obligatorio a propósito. Si el servicio de origen no manda un id estable no hay
     * forma de distinguir una donación nueva de un reintento de la misma, y el proceso
     * opcional era justamente el que daba origen al bug: cada reintento insertaba una fila
     * y volvía a aplicar la regla. Preferimos un 400 explícito a guardar una fila que no se
     * puede deduplicar.
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
