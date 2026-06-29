package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

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
