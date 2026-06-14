package ar.edu.utn.frba.ddsi.incentivos.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ImpactoDonacionDTO {
    private UUID idDonacion;
    private UUID idUsuario;
    private LocalDate fechaEntrega;
    private Integer cantidadBienes;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;
}
