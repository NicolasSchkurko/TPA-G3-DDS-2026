package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;


public class SubcategoriaBien {
    @Getter
    @Setter
    private String nombre;
    private CategoriaBien categoria;

    public SubcategoriaBien(String nombre, CategoriaBien categoria){
        this.nombre = nombre;
        this.categoria = categoria;
    }
}
