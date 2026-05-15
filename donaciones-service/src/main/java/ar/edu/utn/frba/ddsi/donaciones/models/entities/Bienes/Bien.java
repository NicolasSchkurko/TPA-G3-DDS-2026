package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bien {
    private String descripcion;
    private SubcategoriaBien subcategoria;
    @Getter(AccessLevel.NONE)
    private String urlFoto;
    private Integer cantidad;
    private UnidadDeMedida unidadUtilizada;

    public Bien(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadUtilizada){
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.urlFoto = urlFoto;
        this.cantidad = cantidad;
        this.unidadUtilizada = unidadUtilizada;
    }
    //este constructor sirve para cuando no tiene foto el bien
    public Bien(String descripcion, SubcategoriaBien subcategoria, Integer cantidad, UnidadDeMedida unidadUtilizada){
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.cantidad = cantidad;
        this.unidadUtilizada = unidadUtilizada;
    }

    public Optional<String> getUrlFoto() {
        return Optional.ofNullable(this.urlFoto);
    }
}

