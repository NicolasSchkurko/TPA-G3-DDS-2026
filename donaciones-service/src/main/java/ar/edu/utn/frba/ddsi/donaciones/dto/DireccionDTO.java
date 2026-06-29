package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DireccionDTO {
    private String calleUno;
    private String calleDos;
    private Integer altura;
    private Integer piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;
}
