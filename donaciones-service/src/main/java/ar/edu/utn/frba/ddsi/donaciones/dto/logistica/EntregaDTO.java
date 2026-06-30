package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EntregaDTO {
    private List<DonacionResumenDTO> donacionResumen; //peso de cada bien
    private DireccionEntidadDTO entidadBeneficiaria; //quiza solo direccion
}
