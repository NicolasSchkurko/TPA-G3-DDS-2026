package ar.edu.utn.frba.ddsi.incentivos.dto;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
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

    public MisionDTO(String nombreMision, Integer progresoActual, Integer progresoObjetivo){
        this.nombreMision = nombreMision;
        this.progresoActual = progresoActual;
        this.progresoObjetivo = progresoObjetivo;
    }
}