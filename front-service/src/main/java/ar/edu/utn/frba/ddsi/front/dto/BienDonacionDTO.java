package ar.edu.utn.frba.ddsi.front.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class BienDonacionDTO {

    @NotBlank(message = "Completá la descripción de cada bien.")
    private String descripcion;

    @NotBlank(message = "Completá la categoría de cada bien.")
    private String categoria;

    @NotBlank(message = "Completá la subcategoría de cada bien.")
    private String subcategoria;

    @NotNull(message = "Indicá la cantidad de cada bien.")
    @Positive(message = "La cantidad debe ser mayor a cero.")
    private Integer cantidad;

    @NotBlank(message = "Seleccioná la unidad de medida.")
    private String unidadDeMedida;

    @NotBlank(message = "Seleccioná el tipo de bien.")
    private String tipoBien;

    private Boolean usado;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;
}
