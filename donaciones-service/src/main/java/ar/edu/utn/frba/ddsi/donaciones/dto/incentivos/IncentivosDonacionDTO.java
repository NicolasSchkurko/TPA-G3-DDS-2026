package ar.edu.utn.frba.ddsi.donaciones.dto.incentivos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class IncentivosDonacionDTO {
    private LocalDate fechaEntrega;
    private Integer cantidadBienes;
    private String subCategoria;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;
}