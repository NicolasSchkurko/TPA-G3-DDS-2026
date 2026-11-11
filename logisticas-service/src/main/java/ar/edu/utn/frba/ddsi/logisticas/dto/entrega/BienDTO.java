package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BienDTO {
    private UUID idDonacion;
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

    /** Copia los datos del ítem sobre este DTO. El ítem no puede ser {@code null}. */
    public BienDTO aplicar(ItemEntrega item) {
        this.idDonacion = item.getIdDonacion();
        this.cantidad = item.getCantidad();
        this.unidadDeMedida = item.getUnidad() != null ? item.getUnidad().getNombre() : null;
        this.estado = item.getEstado() != null ? item.getEstado().toString() : null;
        this.fechaCambioEstado = item.getFechaCambioEstado();
        this.fotoComprobante = item.getFotoComprobante();
        this.entidadDestino = new DireccionDTO().aplicar(item.getEntidadDestino());
        this.eventos = item.getEventos() == null
                ? List.of()
                : item.getEventos().stream()
                        .map(evento -> new EventoLogisticaDTO().aplicar(evento))
                        .toList();
        return this;
    }
}