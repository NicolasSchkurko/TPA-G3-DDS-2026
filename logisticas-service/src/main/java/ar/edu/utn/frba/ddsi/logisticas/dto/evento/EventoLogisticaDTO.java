package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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

    /** Copia los datos del evento sobre este DTO. El evento no puede ser {@code null}. */
    public EventoLogisticaDTO aplicar(EventoLogistica evento) {
        this.id = evento.getId();
        this.tipoEvento = evento.getTipoEvento();
        this.referenciaId = evento.getReferenciaId();
        this.justificacion = evento.getJustificacion();
        this.payloadJson = evento.getPayloadJson();
        return this;
    }
}