package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActividadMensual {
    private YearMonth periodo;
    private Integer cantidadDonaciones;
    private Integer organizacionesAyudadas;
}
