package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;

import lombok.Getter;

@Getter
public class Metricas {
    private YearMonth periodo;
    private Double variacionPorcentual;

    public Metricas(YearMonth periodo, Double variacionPorcentual) {
        this.periodo = periodo;
        this.variacionPorcentual = variacionPorcentual;
    }

    public static Double calcularVariacion(Number datoActual, Number datoAnterior) {
        if (datoActual == null || datoAnterior == null) return 0.0;
        if (datoAnterior.doubleValue() == 0.0) {
            return 0.0;
        }
        return ((datoActual.doubleValue() - datoAnterior.doubleValue())
                / datoAnterior.doubleValue()) * 100.0;
    }
}
