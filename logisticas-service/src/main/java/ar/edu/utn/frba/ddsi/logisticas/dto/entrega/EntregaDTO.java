package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntregaDTO {
    private BienesDTO donacionResumen; //peso de cada bien
    private DireccionDTO entidadBeneficiaria; //quiza solo direccion
}