package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

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