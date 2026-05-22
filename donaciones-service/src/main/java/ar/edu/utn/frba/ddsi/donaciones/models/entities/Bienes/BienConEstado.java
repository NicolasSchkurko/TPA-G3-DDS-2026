package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BienConEstado extends Bien {
    private boolean usado;

    public BienConEstado(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadUtilizada, boolean usado) {
        super(descripcion, subcategoria, urlFoto, cantidad, unidadUtilizada);
        this.usado = usado;
    }

    public BienConEstado(String descripcion, SubcategoriaBien subcategoria, Integer cantidad, UnidadDeMedida unidadUtilizada, boolean usado) {
        super(descripcion, subcategoria, cantidad, unidadUtilizada);
        this.usado = usado;
    }

    public String toString() {
        return "BienConEstado{descripcion=" + descripcion + ", subcategoria=" + subcategoria + ", urlFoto=" + urlFoto + ", cantidad=" + cantidad + ", unidadUtilizada=" + unidadUtilizada + ", usado=" + usado + '}';
    }

}