package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienConEstado extends Bien {
    private boolean usado;

    public BienConEstado(String descripcion, SubcategoriaBien subcategoria, String foto, Integer cantidad, UnidadDeMedida unidadautilizada, boolean usado) {
        super(descripcion, subcategoria, foto, cantidad, unidadautilizada);
        this.usado = usado;
    }
}