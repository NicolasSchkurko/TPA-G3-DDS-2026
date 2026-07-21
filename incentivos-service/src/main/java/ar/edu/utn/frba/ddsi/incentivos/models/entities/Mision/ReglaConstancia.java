package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import lombok.Getter;
import lombok.Setter;

import java.time.temporal.ChronoUnit;

@Getter
@Setter
public class ReglaConstancia {
    private Integer cantidad;
    private ChronoUnit unidadTiempo;
}
