package ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad que representa un evento ocurrido en el dominio de Logística.
 * Sirve como bitácora (Outbox) para que otros servicios puedan consultarla vía HTTP Polling.
 */
@Getter
@Setter
public class EventoLogistica {

  // Usamos Long (Auto-incremental) para que el Polling sea simple (ej: "dame los mayores a ID 5")
  private Long id;
  private String tipoEvento;
  private LocalDateTime fecha;
  private String payloadJson;

  public EventoLogistica(String tipoEvento, String payloadJson) {
    this.tipoEvento = tipoEvento;
    this.payloadJson = payloadJson;
    this.fecha = LocalDateTime.now();
  }

  public EventoLogistica() {}
}