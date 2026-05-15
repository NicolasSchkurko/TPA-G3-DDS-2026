package ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad{
    private Integer cantidadObjetivo;

    public NecesidadExtraordinaria(SubcategoriaBien subcategoria, String descripcion, Integer cantidadObjetivo){
        super(subcategoria, descripcion);
        this.cantidadObjetivo = cantidadObjetivo;
    }

    @Override
    public boolean estaSatisfecha() {
        return this.cantidadRecibida() >= cantidadObjetivo;
    }
}
