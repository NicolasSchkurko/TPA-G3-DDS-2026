package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representante de una persona jurídica, tal como lo devuelve donaciones-service.
 * Los numéricos son wrappers porque el formulario de administración puede dejarlos vacíos.
 */
@Getter
@Setter
@NoArgsConstructor
public class RepresentanteDTO {
    private String nombre;
    private String apellido;
    private Integer numeroDeDocumento;
    private Integer edad;
    private String genero;
    private Boolean activo;

    public String etiqueta() {
        String completo = ((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim();
        return completo.isBlank() ? "Representante sin nombre" : completo;
    }
}
