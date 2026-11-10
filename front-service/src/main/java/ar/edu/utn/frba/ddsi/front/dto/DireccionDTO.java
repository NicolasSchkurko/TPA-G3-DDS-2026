package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO de dirección alineado con el JSON devuelto por donaciones-service. */
@Getter
@Setter
@NoArgsConstructor
public class DireccionDTO {
    private String calleUno;
    private String calleDos;
    private Integer altura;
    private Integer piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;

    public String etiqueta() {
        String calle = calleUno != null ? calleUno : "";
        String alturaStr = altura != null ? String.valueOf(altura) : "";
        String base = (calle + " " + alturaStr).trim();
        return base.isBlank() ? (ciudad != null ? ciudad : "Sin dirección") : base;
    }
}
