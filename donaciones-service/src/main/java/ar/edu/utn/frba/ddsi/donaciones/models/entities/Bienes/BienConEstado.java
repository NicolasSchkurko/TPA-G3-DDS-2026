package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienConEstado extends Bien {
    private boolean usado;

    public BienConEstado(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadautilizada, boolean usado) {
        super(descripcion, subcategoria, urlFoto, cantidad, unidadautilizada);
        this.usado = usado;
    }
}