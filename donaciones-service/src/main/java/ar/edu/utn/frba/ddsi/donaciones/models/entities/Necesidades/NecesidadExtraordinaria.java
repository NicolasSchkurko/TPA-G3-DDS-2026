package ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {

    public NecesidadExtraordinaria(SubcategoriaBien subcategoria, String descripcion, Integer cantidadObjetivo) {
        super(subcategoria, descripcion, cantidadObjetivo);
    }

    protected NecesidadExtraordinaria() {
        // Constructor requerido por JPA/Hibernate.
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
