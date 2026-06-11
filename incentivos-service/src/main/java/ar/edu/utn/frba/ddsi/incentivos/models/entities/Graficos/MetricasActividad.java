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

    public Double calcularVariacion(Integer dato1, Integer dato2){
        if (dato1 == null || dato2 == null || dato2 == 0) return null;
        return (double) ((double)(dato1 - dato2) / dato2) * 100;
    }

    public MetricasActividadDTO toDTO(){
        MetricasActividadDTO dto = new MetricasActividadDTO();
        dto.setPeriodoActual(this.periodoActual);
        dto.setPeriodoAnterior(this.periodoAnterior);

        dto.setDonacionesActual(this.donacionesActual);
        dto.setDonacionesAnterior(this.donacionesAnterior);
        dto.setVariacionPorcentualDonaciones(this.variacionPorcentualDonaciones);

        dto.setOrganizacionesAyudadasActual(this.organizacionesAyudadasActual);
        dto.setOrganizacionesAyudadasAnterior(this.organizacionesAyudadasAnterior);
        dto.setVariacionPorcentualOrganizaciones(this.variacionPorcentualOrganizaciones);

        return dto;
    }
}
