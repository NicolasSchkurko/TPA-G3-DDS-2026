package ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropuestaAsignacionDTO {
    private EntidadBeneficiariaResumenDTO entidad;
    private NecesidadResumenDTO necesidad;
    private String algoritmo;
    private int posicion;
    private double score;

    public static PropuestaAsignacionDTO from(PropuestaAsignacion propuesta) {
        if (propuesta == null) return null;
        PropuestaAsignacionDTO dto = new PropuestaAsignacionDTO();
        dto.setEntidad(EntidadBeneficiariaResumenDTO.from(propuesta.getEntidad()));
        dto.setNecesidad(NecesidadResumenDTO.from(propuesta.getNecesidad()));
        dto.setAlgoritmo(propuesta.getAlgoritmo());
        dto.setPosicion(propuesta.getPosicion());
        dto.setScore(propuesta.getScore());
        return dto;
    }
}