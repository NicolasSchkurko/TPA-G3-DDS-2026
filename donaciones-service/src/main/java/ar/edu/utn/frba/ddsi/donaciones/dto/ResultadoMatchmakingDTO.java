package ar.edu.utn.frba.ddsi.donaciones.dto;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResultadoMatchmakingDTO {

    private DonacionDTO donacion;

    // Lista unificada de propuestas con sus respectivos metadatos de algoritmos
    private  List<PropuestaAsignacionDTO> propuestasOrdenadas;

    // Flag para la Interfaz de Usuario
    private  boolean huboCoincidenciaTotal;
}
