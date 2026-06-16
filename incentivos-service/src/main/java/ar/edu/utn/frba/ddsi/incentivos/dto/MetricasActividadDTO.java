package ar.edu.utn.frba.ddsi.incentivos.dto;

import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class MetricasActividadDTO {
    private YearMonth periodo;
    private Double variacionPorcentualDonaciones;
    private Double variacionPorcentualOrganizaciones;

    public MetricasActividadDTO(YearMonth periodo, Double variacionPorcentualDonaciones, Double variacionPorcentualOrganizaciones){
        this.periodo = periodo;
        this.variacionPorcentualDonaciones = variacionPorcentualDonaciones;
        this.variacionPorcentualOrganizaciones = variacionPorcentualOrganizaciones;
    }
}