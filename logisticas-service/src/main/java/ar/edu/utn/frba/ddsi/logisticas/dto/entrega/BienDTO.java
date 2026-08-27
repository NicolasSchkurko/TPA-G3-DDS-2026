package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoDTO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienDTO {
    private Integer cantidad;
    private String unidadDeMedida;
    private String estado;
    private LocalDateTime fechaCambioEstado;
    private String fotoComprobante;
    private DireccionDTO entidadDestino;
    private List<EventoDTO> eventos;

    public BienDTO(Integer cantidad, String unidadDeMedida, String estado, LocalDateTime fechaCambioEstado, String fotoComprobante, DireccionDTO entidadDestino, List<EventoDTO> eventos){
        this.cantidad = cantidad;
        this.unidadDeMedida = unidadDeMedida;
        this.estado = estado;
        this.fechaCambioEstado = fechaCambioEstado;
        this.fotoComprobante = fotoComprobante;
        this.entidadDestino = entidadDestino;
        this.eventos = eventos;
    }
}
