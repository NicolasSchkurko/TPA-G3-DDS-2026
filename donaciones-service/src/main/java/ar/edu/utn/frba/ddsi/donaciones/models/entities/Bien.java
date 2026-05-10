package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;


public class Bien {
    @Getter
    @Setter
    private String descripcion;
    private SubcategoriaBien subcategoria;
    private String foto;
    private Integer cantidad;
    private UnidadDeMedida unidadautilizada;

    public Bien(String descripcion, SubcategoriaBien subcategoria, String foto, Integer cantidad, UnidadDeMedida unidadautilizada){
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.foto = foto;
        this.cantidad = cantidad;
        this.unidadautilizada = unidadautilizada;
    }
}

