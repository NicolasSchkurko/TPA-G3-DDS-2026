package ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /** Optimistic locking: una escritura concurrente tira OptimisticLockingFailureException en vez de pisar. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

  /** El ítem al que pertenece el evento (lado dueño; el lado muchos es
   *  {@code ItemEntrega.eventos}, mapeado por {@code mappedBy} sin {@code @JoinColumn}).
   *  Nullable a propósito: el {@code INICIO_RUTA} es de la ruta y lo dice su {@code referenciaId}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_donacion", referencedColumnName = "id_donacion")
    @JsonIgnore
    private ItemEntrega item;

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