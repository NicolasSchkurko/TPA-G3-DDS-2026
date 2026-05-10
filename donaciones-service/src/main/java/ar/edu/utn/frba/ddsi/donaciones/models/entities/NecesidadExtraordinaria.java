package ar.edu.utn.frba.ddsi.donaciones.models.entities;

public class NecesidadExtraordinaria extends Necesidad{
    private Integer cantidadRecibida;
    private Integer cantidad;

    public NecesidadExtraordinaria(SubcategoriaBien subcategoria, String descripcion, Integer cantidadRecibida, Integer cantidad){
        super(subcategoria, descripcion);
        this.cantidadRecibida = cantidadRecibida;
        this.cantidad = cantidad;
    }
}
