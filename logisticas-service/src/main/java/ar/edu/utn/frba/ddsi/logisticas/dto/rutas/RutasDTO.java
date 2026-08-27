package ar.edu.utn.frba.ddsi.logisticas.dto.rutas;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RutasDTO {
    private List<RutaDTO> rutas;

    public RutasDTO(List<RutaDTO> rutas){
        this.rutas = rutas;
    }
}
