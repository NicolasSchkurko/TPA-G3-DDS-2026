package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class MisionDTO {
    private List<ImpactoDonacionDTO> donaciones;
    private String nombreMision;
    private InsigniaDTO insigniaObjetivo;
    private Integer progresoActual;
    private Integer progresoObjetivo;
}
