package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Un evento de logística tal como viaja por el broker.
 *
 *  <p>Existe porque {@code EventoLogistica} lleva el payload serializado a mano: mandar la entidad
 *  sería un JSON dentro de un JSON. Los nombres de los campos son contrato con donaciones-service.
 *  */
@Getter
@Setter
@NoArgsConstructor
public class EventoLogisticaMensajeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Id del evento. Sirve de cursor para el consumidor: no reprocesa lo que ya vio. */
    private Long id;

    /** INICIO_RUTA, ENTREGA_CONFIRMADA, ENTREGA_FALLIDA o REINGRESO_DEPOSITO (contrato del consumidor). */
    private String tipoEvento;

    /** Id de la ruta o de la donación, según el tipo. */
    private String referenciaId;

    /** Momento del evento. */
    private LocalDateTime fecha;

    /** Motivo, solo en los eventos que lo llevan. */
    private String justificacion;

    /** Payload del evento, ya serializado: el receptor no comparte modelo con logística. */
    private String payloadJson;

    public static EventoLogisticaMensajeDTO desde(
            ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica e) {
        EventoLogisticaMensajeDTO dto = new EventoLogisticaMensajeDTO();
        dto.id = e.getId();
        dto.tipoEvento = e.getTipoEvento();
        dto.referenciaId = e.getReferenciaId();
        dto.fecha = e.getFecha();
        dto.justificacion = e.getJustificacion();
        dto.payloadJson = e.getPayloadJson();
        return dto;
    }
}
