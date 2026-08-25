package ar.edu.utn.frba.ddsi.logisticas.dto.camion;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CamionesDTO {
    private List<CamionDTO> camiones;

    public CamionesDTO(List<CamionDTO> camiones){
        this.camiones = camiones;
    }
}
