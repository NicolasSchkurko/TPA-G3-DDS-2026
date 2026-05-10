package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


public class Necesidad {
    @Getter
    @Setter
    private SubcategoriaBien subcategoria;
    private List<Donacion> donaciones;
    private String descripcion;

    public Necesidad(SubcategoriaBien subcategoria, List<Donacion> donaciones, String descripcion){
        this.subcategoria = subcategoria;
        this.descripcion = descripcion;
        this.donaciones = donaciones;
    }
}
