package ar.edu.utn.frba.ddsi.notificaciones.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lo que viaja por el broker.
 *
 * <p><b>Existe porque la entidad no sirve para transportarse.</b> {@code Notificacion} es una
 * entidad JPA: su id es un {@code UUID} que se regenera al deserializar, tiene una relación
 * {@code @OneToOne} y un enum persistido. Si se manda la entidad, el consumidor recibe un
 * objeto con un id distinto del que se guardó y el {@code save} de respuesta termina
 * actualizando la fila de otro aviso, o insertando una que no existe.
 *
 * <p>Además el contrato queda explícito: si mañana la entidad gana una columna interna, el
 * broker no cambia porque no la está transportando.
 *
 * <p>Implementa {@link Serializable} porque es lo que exige el contrato de AMQP y porque
 * {@code SimpleMessageConverter} —el converter por defecto de Boot— solo serializa
 * {@code Serializable}. Con el {@code Jackson2JsonMessageConverter} declarado en
 * {@code RabbitConfig} ya no depende de eso, pero dejarlo implementable evita que el día que
 * alguien saque ese bean el fallo aparezca solo en runtime.
 */
@Getter
@Setter
@NoArgsConstructor
public class NotificacionMensajeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Identificador de la notificación ya persistida, para que el consumidor la actualice. */
    private String id;

    private String medioDeContacto;

    private String direccionDeContacto;

    private String asuntoMensaje;

    private String cuerpoMensaje;

    private LocalDateTime fechaCreacion;

    /**
     * Construye el mensaje a partir de la entidad guardada.
     *
     * <p>El estado no viaja: el consumidor decide y lo marca. Si el productor mandara
     * {@code PENDIENTE} o {@code ENVIADA}, un reintento podría dejar el aviso marcado como
     * enviado sin que nadie lo haya enviado.
     */
    public static NotificacionMensajeDTO desde(
            ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion n) {
        NotificacionMensajeDTO dto = new NotificacionMensajeDTO();
        dto.id = n.getId().toString();
        dto.medioDeContacto = n.getTipoMedioDeContacto();
        dto.direccionDeContacto = n.getDireccionDeContacto();
        dto.fechaCreacion = n.getFechaCreacion();

        if (n.getMensaje() != null) {
            dto.asuntoMensaje = n.getMensaje().getAsunto();
            dto.cuerpoMensaje = n.getMensaje().getCuerpo();
        }

        return dto;
    }
}