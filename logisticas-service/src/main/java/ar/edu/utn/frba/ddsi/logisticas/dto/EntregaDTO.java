package ar.edu.utn.frba.ddsi.logisticas.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EntregaDTO {
    private List<BienDTO> donacionResumen; //peso de cada bien
    private DireccionDTO entidadBeneficiaria; //quiza solo direccion
}
