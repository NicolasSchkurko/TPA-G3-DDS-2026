package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;


public class BienConEstado extends Bien {
    @Getter
    @Setter
    private boolean usado;

    public BienConEstado(String descripcion, SubcategoriaBien subcategoria, String foto, Integer cantidad, UnidadDeMedida unidadautilizada, boolean usado) {
        super(descripcion, subcategoria, foto, cantidad, unidadautilizada);
        this.usado = usado;
    }
}