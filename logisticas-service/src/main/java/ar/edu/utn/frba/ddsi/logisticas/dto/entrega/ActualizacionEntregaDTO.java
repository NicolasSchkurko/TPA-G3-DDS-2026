package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizacionEntregaDTO {
  private String estado; // "ENTREGADA", "NO_RECIBIDA", "PENDIENTE"
  private String fotoUrl; // Requerido si es ENTREGADA
  private String justificacion; // Requerido si es NO_RECIBIDA
}