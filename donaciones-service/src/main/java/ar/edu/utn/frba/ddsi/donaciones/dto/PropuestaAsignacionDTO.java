package ar.edu.utn.frba.ddsi.donaciones.dto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.Necesidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropuestaAsignacionDTO {
    private EntidadBeneficiariaDTO entidad;
    private NecesidadDTO necesidad;

    private String algoritmo;
    private int posicion;
    private double score;
}
