package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoDTO {
    private Long id;
    private String tipoEvento;
    private LocalDateTime fecha;
    private String referenciaId;
    private String justificacion;
    private String payloadJson;

    public EventoDTO(Long id,
                     String tipoEvento,
                     LocalDateTime fecha,
                     String referenciaId,
                     String justificacion,
                     String payloadJson){
        this.id = id;
        this.tipoEvento = tipoEvento;
        this.fecha = fecha;
        this.referenciaId = referenciaId;
        this.justificacion = justificacion;
        this.payloadJson = payloadJson;
    }
}
