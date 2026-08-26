package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventosDTO {
    private List<EventoDTO> eventos;

    public EventosDTO(List<EventoDTO> eventos){
        this.eventos = eventos;
    }
}
