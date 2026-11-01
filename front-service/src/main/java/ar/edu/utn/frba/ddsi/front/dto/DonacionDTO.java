package ar.edu.utn.frba.ddsi.front.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonacionDTO {
    private UUID id;
    private String donanteName;
    private String entidadBeneficiaria;
    private String descripcion;
    private String estado;
    private String subcategoriaName;
    private String categoriaBienName;
    private LocalDate fechaEntrega;
    private Integer cantidadTotalBienes;
}
