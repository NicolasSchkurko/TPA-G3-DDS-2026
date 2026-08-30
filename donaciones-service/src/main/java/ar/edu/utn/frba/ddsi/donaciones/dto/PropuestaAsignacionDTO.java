package ar.edu.utn.frba.ddsi.donaciones.dto;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropuestaAsignacionDTO {
    private EntidadBeneficiariaDTO entidad;
    private NecesidadDTO necesidad;
    private String algoritmo;
    private int posicion;
    private double score;

    public static PropuestaAsignacionDTO from(PropuestaAsignacion propuesta) {
        if (propuesta == null) return null;
        PropuestaAsignacionDTO dto = new PropuestaAsignacionDTO();
        dto.setEntidad(EntidadBeneficiariaDTO.from(propuesta.getEntidad()));
        dto.setNecesidad(NecesidadDTO.from(propuesta.getNecesidad()));
        dto.setAlgoritmo(propuesta.getAlgoritmo());
        dto.setPosicion(propuesta.getPosicion());
        dto.setScore(propuesta.getScore());
        return dto;
    }
}