package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetricasHistoricasDTO {
    //retorna comparacion de la actividad de un perfil desde su creacion
    private List<MetricaDTO> metricasPerfil;

    public MetricasHistoricasDTO(List<MetricaDTO> metricasPerfil){
        this.metricasPerfil = metricasPerfil;
    }
}