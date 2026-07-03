package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EventoLogisticaDTO {
  private Long id;
  private String tipoEvento;
  private LocalDateTime fecha;
  private String payloadJson; // Acá viene la info variable según el evento
}