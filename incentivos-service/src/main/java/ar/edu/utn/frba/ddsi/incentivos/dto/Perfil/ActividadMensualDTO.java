package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;
import java.util.List;

@Getter
@Setter

public class ActividadMensualDTO {
    private YearMonth periodo;
    private List<MetricasActividadDTO> metricas;

    public ActividadMensualDTO(YearMonth periodo, List<MetricasActividadDTO> metricas){
        this.periodo = periodo;
        this.metricas = metricas;
    }
}