package ar.edu.utn.frba.ddsi.incentivos.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ImpactoDonacionDTO {
    private LocalDate fechaEntrega;
    private Integer cantidadBienes;
    private String subCategoria;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;
}
