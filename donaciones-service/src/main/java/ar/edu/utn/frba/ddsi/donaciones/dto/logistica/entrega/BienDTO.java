package ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega;

import lombok.Getter;
import lombok.Setter;

/**
 * Cómo viaja un bien en el mensaje de integración con logística (donaciones-service -> logística).
 *
 * <p>Logística tiene su propio {@code BienDTO} con campos adicionales (estado, fechaCambioEstado,
 * fotoComprobante, entidadDestino, eventos) que ella misma completa al procesar la entrega. Antes
 * esos campos estaban copiados acá también, con un constructor de 7 parámetros que donaciones-service
 * nunca invocaba (siempre mandaba null): este DTO solo modela lo que donaciones-service efectivamente
 * publica, no el modelo de dominio de logística.
 */
@Getter
@Setter
public class BienDTO {
    private Integer cantidad;
    private String unidadDeMedida;

    public BienDTO(Integer cantidad, String unidadDeMedida) {
        this.cantidad = cantidad;
        this.unidadDeMedida = unidadDeMedida;
    }
}