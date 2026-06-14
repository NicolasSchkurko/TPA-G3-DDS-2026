package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;

import ar.edu.utn.frba.ddsi.incentivos.dto.MetricasActividadDTO;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter

public class MetricasActividad {
    private YearMonth periodoActual;
    private YearMonth periodoAnterior;

    private Integer donacionesActual;
    private Integer donacionesAnterior;
    private Double variacionPorcentualDonaciones;

    private Integer organizacionesAyudadasActual;
    private Integer organizacionesAyudadasAnterior;
    private Double variacionPorcentualOrganizaciones;

    public MetricasActividad(ActividadMensual actual, ActividadMensual anterior) {
        this.periodoActual = actual.getPeriodo();
        this.donacionesActual = actual.getCantidadDonaciones();
        this.organizacionesAyudadasActual = actual.getOrganizacionesAyudadas();

        if (anterior != null) {
            this.periodoAnterior = anterior.getPeriodo();
            this.donacionesAnterior = anterior.getCantidadDonaciones();
            this.organizacionesAyudadasAnterior = anterior.getOrganizacionesAyudadas();
        } else {
            this.periodoAnterior = this.periodoActual.minusMonths(1);
            this.donacionesAnterior = 0;
            this.organizacionesAyudadasAnterior = 0;
        }

        this.variacionPorcentualDonaciones = calcularVariacion(this.donacionesActual, this.donacionesAnterior);
        this.variacionPorcentualOrganizaciones = calcularVariacion(this.organizacionesAyudadasActual, this.organizacionesAyudadasAnterior);
    }

    private Double calcularVariacion(Integer dato1, Integer dato2){
        if (dato1 == null || dato2 == null) return 0.0;
        if (dato2 == 0) {
            return dato1 > 0 ? 100.0 : 0.0;
        }
        return ((double)(dato1 - dato2) / dato2) * 100.0;
    }


}
