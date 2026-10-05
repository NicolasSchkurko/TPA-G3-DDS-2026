package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConstanciaDTO {

    @NotNull(message = "La constancia requiere una cantidad de unidades")
    @Positive(message = "La cantidad de la constancia debe ser mayor a cero")
    private Integer cantidad;

    @NotBlank(message = "La constancia requiere una unidad de tiempo")
    private String unidadTiempo;

    public ConstanciaDTO(Integer cant,
                         String unidad) {
        this.cantidad = cant;
        this.unidadTiempo = unidad;
    }
}
