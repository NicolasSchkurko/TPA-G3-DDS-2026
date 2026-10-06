package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReglaDTO {

    /**
     * Opcional: solo hay missions con constancia (por ejemplo "Racha"). Si viene, se
     * valida en cascada con {@link ConstanciaDTO}.
     */
    @Valid
    private ConstanciaDTO constancia;

    @NotBlank(message = "La regla requiere un atributo de impacto")
    private String atributo;

    @NotNull(message = "La regla requiere una operación")
    @Valid
    private OperacionDTO operacion;

    public ReglaDTO(ConstanciaDTO constancia,
                    String atributo,
                    OperacionDTO operacion) {
        this.constancia = constancia;
        this.atributo = atributo;
        this.operacion = operacion;
    }
}
