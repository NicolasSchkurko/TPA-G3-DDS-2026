package ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad que representa un evento ocurrido en el dominio de Logística.
 * Sirve como bitácora (Outbox) para que otros servicios puedan consultarla vía HTTP Polling.
 */
@Entity
@Table(name = "evento_logistica")
@Getter
@Setter
@NoArgsConstructor
public class EventoLogistica {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_evento")
  private Long id;

  @Column(name = "tipo_evento", nullable = false)
  private String tipoEvento;

  @Column(name = "fecha", nullable = false)
  private LocalDateTime fecha;

  // Agregados para soportar la trazabilidad completa y las justificaciones
  @Column(name = "referencia_id")
  private String referenciaId;

  @Column(name = "justificacion")
  private String justificacion;

  @Column(name = "payload_json", columnDefinition = "TEXT")
  private String payloadJson;

  public EventoLogistica(String tipoEvento, String referenciaId, LocalDateTime fecha, String justificacion) {
    this.tipoEvento = tipoEvento;
    this.referenciaId = referenciaId;
    this.fecha = fecha != null ? fecha : LocalDateTime.now();
    this.justificacion = justificacion;
  }

  public EventoLogistica(String tipoEvento, String payloadJson) {
    this.tipoEvento = tipoEvento;
    this.payloadJson = payloadJson;
    this.fecha = LocalDateTime.now();
  }
}