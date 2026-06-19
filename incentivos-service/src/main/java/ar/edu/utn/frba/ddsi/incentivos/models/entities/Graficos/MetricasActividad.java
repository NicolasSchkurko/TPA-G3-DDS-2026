package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class MetricasActividad {
    private YearMonth periodo;
    private Double variacionPorcentualDonaciones;
    private Double variacionPorcentualOrganizaciones;

    public MetricasActividad(ActividadMensual actual, ActividadMensual anterior) {
        this.periodo = YearMonth.now();
        this.variacionPorcentualDonaciones = calcularVariacion(actual.getCantidadDonaciones(), anterior.getCantidadDonaciones());
        this.variacionPorcentualOrganizaciones = calcularVariacion(actual.getOrganizacionesAyudadas(), anterior.getOrganizacionesAyudadas());
    }

    private Double calcularVariacion(Integer dato1, Integer dato2){
        if (dato1 == null || dato2 == null) return 0.0;
        if (dato2 == 0) {
            return 0.0;
        }
        return ((double)(dato1 - dato2) / dato2) * 100.0;
    }
}