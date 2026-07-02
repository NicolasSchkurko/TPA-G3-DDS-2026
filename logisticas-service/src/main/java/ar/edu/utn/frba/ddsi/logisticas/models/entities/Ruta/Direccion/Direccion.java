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

    public Direccion(String calle1, String calle2, Integer altura, Integer piso, String departamento, String nombreCiudad, String nombreProvincia, String nombrePais){
        this.calle1 = calle1;
        this.calle2 = calle2;
        this.altura = altura;
        this.piso = piso;
        this.departamento = departamento;
        Pais pais = new Pais();
        pais.setNombre(nombrePais);
        Provincia provincia = new Provincia();
        provincia.setNombre(nombreProvincia);
        provincia.setPais(pais);
        this.ciudad = new Ciudad(nombreCiudad, provincia);
    }
}