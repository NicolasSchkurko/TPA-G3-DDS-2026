package ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EntregaDTO {
    private BienesDTO donacionResumen; //peso de cada bien
    private DireccionDTO entidadBeneficiaria; //quiza solo direccion

    public EntregaDTO(List<UUID> idsDonaciones, List<BienDTO> bienes, DireccionDTO entidadBeneficiaria){
        this.donacionResumen = new BienesDTO(idsDonaciones, bienes);
        this.entidadBeneficiaria = entidadBeneficiaria;
    }
}