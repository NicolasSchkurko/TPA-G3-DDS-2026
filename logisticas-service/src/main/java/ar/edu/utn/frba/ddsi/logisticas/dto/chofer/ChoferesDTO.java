package ar.edu.utn.frba.ddsi.logisticas.dto.chofer;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ChoferesDTO {
    private List<ChoferDTO> choferes;

    public ChoferesDTO() {
    }

    public ChoferesDTO(List<ChoferDTO> choferes){
        this.choferes = choferes;
    }
}