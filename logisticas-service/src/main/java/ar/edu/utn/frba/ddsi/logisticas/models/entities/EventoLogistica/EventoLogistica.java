package ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Entidad que representa un evento ocurrido en el dominio de Logística.
 * Sirve como bitácora (Outbox) para que otros servicios puedan consultarla vía HTTP Polling.
 */
@Document(collection = "evento_logistica")
@Getter
@Setter
@NoArgsConstructor
public class EventoLogistica {

  @Id
  @Field(name = "id_evento")
  private Long id;

  @Version
  private Long version;

  @Field(name = "tipo_evento")
  private String tipoEvento;

  @Field(name = "fecha")
  private LocalDateTime fecha;

  // Agregados para soportar la trazabilidad completa y las justificaciones
  @Field(name = "referencia_id")
  private String referenciaId;

  @Field(name = "justificacion")
  private String justificacion;

  @Field(name = "payload_json")
  private String payloadJson;

  /** El ítem al que pertenece el evento (lado dueño; el lado muchos es
   *  {@code ItemEntrega.eventos}, mapeado por {@code mappedBy} sin {@code @JoinColumn}).
   *  Nullable a propósito: el {@code INICIO_RUTA} es de la ruta y lo dice su {@code referenciaId}. */

  public EventoLogistica(String tipoEvento, String referenciaId, LocalDateTime fecha, String justificacion) {
    this.id = Secuencia.siguiente();
    this.tipoEvento = tipoEvento;
    this.referenciaId = referenciaId;
    this.fecha = fecha != null ? fecha : LocalDateTime.now();
    this.justificacion = justificacion;
  }

  public EventoLogistica(String tipoEvento, String payloadJson) {
    this.id = Secuencia.siguiente();
    this.tipoEvento = tipoEvento;
    this.payloadJson = payloadJson;
    this.fecha = LocalDateTime.now();
  }
}