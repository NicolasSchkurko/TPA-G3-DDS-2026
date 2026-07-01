package ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Direccion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Direccion {
    private String calle1;
    private String calle2;
    private Integer altura;
    private Boolean sinAltura;
    private Integer piso;
    private String departamento;
    private Ciudad ciudad;
}