package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoLogisticaDTO {
    private Long id;
    private String tipoEvento;
    private String referenciaId;
    private String justificacion;
    private String payloadJson;

    public EventoLogisticaDTO(Long id,
                              String tipoEvento,
                              String referenciaId,
                              String justificacion,
                              String payloadJson){
        this.id = id;
        this.tipoEvento = tipoEvento;
        this.referenciaId = referenciaId;
        this.justificacion = justificacion;
        this.payloadJson = payloadJson;
    }
}