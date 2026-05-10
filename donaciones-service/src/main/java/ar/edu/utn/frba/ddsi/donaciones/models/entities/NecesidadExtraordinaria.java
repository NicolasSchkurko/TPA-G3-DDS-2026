package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;


public class NecesidadExtraordinaria extends Necesidad{
    @Getter
    @Setter
    private Integer cantidadRecibida;
    private Integer cantidad;

    public NecesidadExtraordinaria(SubcategoriaBien subcategoria, String descripcion, Integer cantidadRecibida, Integer cantidad){
        super(subcategoria, descripcion);
        this.cantidadRecibida = cantidadRecibida;
        this.cantidad = cantidad;
    }
}
