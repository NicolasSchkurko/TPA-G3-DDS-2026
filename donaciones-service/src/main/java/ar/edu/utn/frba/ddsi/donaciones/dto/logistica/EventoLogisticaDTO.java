package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EventoLogisticaDTO {
  private Long id;
  private String tipoEvento;
  private String referenciaId;
  private String justificacion;
  private String payloadJson;
}