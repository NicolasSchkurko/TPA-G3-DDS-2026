package ar.edu.utn.frba.ddsi.donaciones.models.entities;

public class Necesidad {
    private SubcategoriaBien subcategoria;
    private String descripcion;

    public Necesidad(SubcategoriaBien subcategoria, String descripcion){
        this.subcategoria = subcategoria;
        this.descripcion = descripcion;
    }
}
