package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class BienesDTO {
    private List<UUID> idsDonaciones;
    private List<BienDTO> bienes;

    public BienesDTO(List<UUID> idsDonaciones, List<BienDTO> bienes){
        this.idsDonaciones = idsDonaciones;
        this.bienes = bienes;
    }
}
