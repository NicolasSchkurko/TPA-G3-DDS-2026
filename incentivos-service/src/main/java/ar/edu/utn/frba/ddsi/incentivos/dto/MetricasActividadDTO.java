package ar.edu.utn.frba.ddsi.incentivos.dto;

import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class MetricasActividadDTO {
    private YearMonth periodoActual;
    private YearMonth periodoAnterior;

    private Integer donacionesActual;
    private Integer donacionesAnterior;
    private Double variacionPorcentualDonaciones;

    private Integer organizacionesAyudadasActual;
    private Integer organizacionesAyudadasAnterior;
    private Double variacionPorcentualOrganizaciones;
}