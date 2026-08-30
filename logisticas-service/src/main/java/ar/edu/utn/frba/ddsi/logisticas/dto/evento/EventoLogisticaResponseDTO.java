package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoLogisticaResponseDTO {
    private List<EventoLogisticaDTO> eventos;

    public EventoLogisticaResponseDTO(List<EventoLogisticaDTO> eventos){
        this.eventos = eventos;
    }
}