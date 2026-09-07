package ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad;

import lombok.Getter;

import java.time.YearMonth;

/** Resultado de la variacion entre dos periodos consecutivos. */
@Getter
public class MetricaActividad {
    private final YearMonth inicio;
    private final YearMonth fin;
    private final Double variacion;

    public MetricaActividad(YearMonth inicio, YearMonth fin, Double variacion) {
        this.inicio = inicio;
        this.fin = fin;
        this.variacion = variacion;
    }

}
