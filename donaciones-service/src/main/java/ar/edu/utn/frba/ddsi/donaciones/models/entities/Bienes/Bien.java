package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import java.util.Optional;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Bien {
    UUID id = UUID.randomUUID();
    String descripcion;
    SubcategoriaBien subcategoria;
    @Getter(AccessLevel.NONE)
    String urlFoto;
    Integer peso;
    UnidadDeMedida unidadUtilizada;

    public Bien(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadUtilizada){
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.urlFoto = urlFoto;
        this.peso = cantidad;
        this.unidadUtilizada = unidadUtilizada;
    }

    public Optional<String> getUrlFoto() {
        return Optional.ofNullable(this.urlFoto);
    }
}

