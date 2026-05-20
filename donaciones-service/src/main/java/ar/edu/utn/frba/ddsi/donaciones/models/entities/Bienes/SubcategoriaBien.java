package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubcategoriaBien {
    private String nombre;
    private CategoriaBien categoria;

    public SubcategoriaBien(String nombre, CategoriaBien categoria){
        this.nombre = nombre;
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return "SubcategoriaBien{nombre=" + nombre + ", categoria=" + categoria + '}';
    }
}
