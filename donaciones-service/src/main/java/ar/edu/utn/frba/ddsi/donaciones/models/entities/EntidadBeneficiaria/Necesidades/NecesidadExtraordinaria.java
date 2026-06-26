package ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {

    public NecesidadExtraordinaria(SubcategoriaBien subcategoria, String descripcion, Integer cantidadObjetivo) {
        super(subcategoria, descripcion, cantidadObjetivo);
    }

    @Override
    public boolean estaSatisfecha() {
        return this.cantidadRecibida() >= cantidadObjetivo;
    }

    @Override
    public String toString() {
        return "NecesidadExtraordinaria{subcategoria=" + subcategoria + ", descripcion=" + descripcion + ", cantidadObjetivo=" + cantidadObjetivo + '}';
    }
}
