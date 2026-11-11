package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DireccionBeneficiariaDTO {
    private String calleUno;
    private Integer altura;
    private String ciudad;
    private String provincia;
    private String pais;
    private Double latitud;
    private Double longitud;
}
