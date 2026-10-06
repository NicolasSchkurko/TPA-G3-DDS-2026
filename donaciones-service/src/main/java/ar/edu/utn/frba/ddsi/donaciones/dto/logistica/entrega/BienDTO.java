package ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BienDTO {
    private Integer cantidad;
    private String unidadDeMedida;
    private String estado;
    private LocalDateTime fechaCambioEstado;
    private String fotoComprobante;
    private DireccionDTO entidadDestino;
    private List<EventoLogisticaDTO> eventos;

    public BienDTO(Integer cantidad, String unidadDeMedida, String estado, LocalDateTime fechaCambioEstado, String fotoComprobante, DireccionDTO entidadDestino, List<EventoLogisticaDTO> eventos){
        this.cantidad = cantidad;
        this.unidadDeMedida = unidadDeMedida;
        this.estado = estado;
        this.fechaCambioEstado = fechaCambioEstado;
        this.fotoComprobante = fotoComprobante;
        this.entidadDestino = entidadDestino;
        this.eventos = eventos;
    }
}