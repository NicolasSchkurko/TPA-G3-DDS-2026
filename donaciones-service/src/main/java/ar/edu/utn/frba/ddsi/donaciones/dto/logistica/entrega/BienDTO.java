package ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** Cómo viaja un bien en el mensaje de integración con logística.
 *  Describe datos de logística (estado, comprobante, eventos); es la copia que serializa
 *  hacia el broker, y el {@code Jackson2JsonMessageConverter} la empareja con la del otro
 *  lado por los nombres de campo (punto 28: conviene separarlas a propósito). */
@Getter
@Setter
public class BienDTO {
    private Integer cantidad;
    private String unidadDeMedida;

    /** Para el mensaje de integración: solo cantidad y unidad; el resto lo llena logística. */
    public BienDTO(Integer cantidad, String unidadDeMedida) {
        this.cantidad = cantidad;
        this.unidadDeMedida = unidadDeMedida;
    }
}