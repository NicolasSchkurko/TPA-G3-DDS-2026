package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;

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
        return (double) ((dato1 - dato2) / dato2) * 100;
    }
}
