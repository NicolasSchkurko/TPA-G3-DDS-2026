package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Un evento de logística tal como viaja por el broker.
 *
 * <p><b>Existe porque {@code EventoLogistica} no sirve para transportarse.</b> La entidad
 * tiene el payload serializado a mano en un {@code String} con un {@code ObjectMapper}, así
 * que mandarla por Rabbit significaría mandar un JSON dentro de un JSON, y el consumidor
 * tendría que adivinar el formato del {@code payloadJson}. Este DTO aplana los datos: lo que
 * el consumidor necesita para notificar, en campos con nombre.
 *
 * <p>El nombre de los campos es el contrato con `donaciones-service`: su
 * {@code EventoLogisticaDTO} lee exactamente estos nombres, y por eso el
 * {@code Jackson2JsonMessageConverter} los empareja sin configuración de por medio.
 */
@Getter
@Setter
@NoArgsConstructor
public class EventoLogisticaMensajeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Id del evento. Sirve de cursor para el consumidor: no reprocesa lo que ya vio. */
    private Long id;

    /**
     * INICIO_RUTA, ENTREGA_CONFIRMADA, ENTREGA_FALLIDA o REINGRESO_DEPOSITO.
     *
     * <p>El nombre del campo tiene que coincidir exactamente con el del DTO del consumidor
     * ({@code tipoEvento}), porque es lo que Jackson usa para emparejar el JSON. Con
     * {@code tipo} en un lado y {@code tipoEvento} en el otro, el mensaje llega al listener y
     * falla con "Failed to convert message", que es el error menos descriptivo que hay.
     */
    private String tipoEvento;

    /** Id de la ruta o de la donación, según el tipo. */
    private String referenciaId;

    /** Nombre del campo que espera el consumidor. */
    private LocalDateTime fecha;

    /** Motivo, solo en los eventos que lo llevan. */
    private String justificacion;

    /**
     * Payload del evento, ya serializado por el productor.
     *
     * <p>Viaja como string y no como entidad porque el receptor no comparte modelo con
     * logistica y no debe: lo que le interesa es el id de la donacion para buscar al donante y
     * a la entidad, mas la URL de seguimiento en el caso de inicio de ruta.
     */
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
