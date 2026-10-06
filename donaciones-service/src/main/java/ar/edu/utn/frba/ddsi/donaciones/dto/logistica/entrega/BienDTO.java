package ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
/**
 * Cómo viaja un bien en el mensaje de integración con logística.
 *
 * <p>Este DTO está en el paquete de `donaciones-service` pero describe datos de logística
 * (estado de entrega, foto de comprobante, eventos). Es la copia que se usa para serializar
 * hacia el broker; logística tiene la suya, con los mismos nombres de campo, y por eso el
 * {@code Jackson2JsonMessageConverter} las empareja sin configuración extra.
 */
public class BienDTO {
    private Integer cantidad;
    private String unidadDeMedida;
    private String estado;
    private LocalDateTime fechaCambioEstado;
    private String fotoComprobante;
    private DireccionDTO entidadDestino;
    private List<EventoLogisticaDTO> eventos;

    /**
     * Constructor mínimo para el mensaje de integración.
     *
     * <p>Solo cantidad y unidad: son los dos datos que logística necesita para crear el ítem
     * de entrega. El estado y los eventos los lleva logística, no este servicio, así que acá
     * se mandan en null y es logística quien los completa al procesar.
     */
    public BienDTO(Integer cantidad, String unidadDeMedida) {
        this.cantidad = cantidad;
        this.unidadDeMedida = unidadDeMedida;
    }

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