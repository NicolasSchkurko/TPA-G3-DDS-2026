package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudEventosDTO {
    private Long desdeId;

    public SolicitudEventosDTO() {
    }

    public SolicitudEventosDTO(Long desdeId) {
        this.desdeId = desdeId;
    }
}