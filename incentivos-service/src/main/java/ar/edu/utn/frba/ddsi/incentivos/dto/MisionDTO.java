package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class MisionDTO {
    private String nombreMision;
    private InsigniaDTO insigniaObjetivo;
    private Integer progresoActual;
    private Integer progresoObjetivo;
}
