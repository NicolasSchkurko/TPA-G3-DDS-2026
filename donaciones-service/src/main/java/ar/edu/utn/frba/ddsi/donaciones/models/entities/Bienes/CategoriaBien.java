package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoriaBien {
    private String nombre;
    private List<SubcategoriaBien> subcategorias;

    public CategoriaBien(String nombre) {
        this.nombre = nombre;
        this.subcategorias = new ArrayList<>();
    }

    public void agregarSubcategoria(SubcategoriaBien subcategoria) {
        this.subcategorias.add(subcategoria);
    }

    @Override
    public String toString() {
        return "CategoriaBien{nombre=" + nombre + '}';
    }
}
