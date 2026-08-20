package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActividadMesDTO {
    private List<ActividadMensualDTO> metricas;

    public ActividadMesDTO(List<ActividadMensualDTO> metricas){
        this.metricas = metricas;
    }
}
