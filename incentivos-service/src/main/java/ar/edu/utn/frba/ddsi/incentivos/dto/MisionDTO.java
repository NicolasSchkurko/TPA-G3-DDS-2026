package ar.edu.utn.frba.ddsi.incentivos.dto;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class MisionDTO {
    private List<DonacionDTO> donaciones;
    private String nombreMision;
    private InsigniaDTO insigniaObjetivo;
    private Integer progresoActual;
    private Integer progresoObjetivo;
}
