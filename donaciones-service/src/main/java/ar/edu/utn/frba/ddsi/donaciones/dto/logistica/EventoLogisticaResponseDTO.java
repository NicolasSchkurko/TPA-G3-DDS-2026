package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class EventoLogisticaResponseDTO {
  // El nombre del atributo DEBE coincidir con la clave del JSON ("eventos")
  private List<EventoLogisticaDTO> eventos;
}