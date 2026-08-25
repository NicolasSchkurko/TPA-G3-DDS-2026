package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EntregaDTO {
    private List<UUID> idsDonaciones;
    private BienesDTO donacionResumen; //peso de cada bien
    private DireccionDTO entidadBeneficiaria; //quiza solo direccion
}