package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Bien {
    @Id
    UUID id = UUID.randomUUID();

    String descripcion;

    // Catálogo compartido (igual que Necesidad.subcategoria): se resuelve vía
    // GestorNecesidades.obtenerOCrearSubcategoria antes de construir el Bien, sin cascade,
    // para no duplicar CategoriaBien/SubcategoriaBien en cada alta.
    @ManyToOne
    @JoinColumn(name = "subcategoria_id")
    SubcategoriaBien subcategoria;

    @Getter(AccessLevel.NONE)
    String urlFoto;

    Integer peso;

    @Enumerated(EnumType.STRING)
    UnidadDeMedida unidadUtilizada;

    public Bien(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadUtilizada){
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.urlFoto = urlFoto;
        this.peso = cantidad;
        this.unidadUtilizada = unidadUtilizada;
    }

    protected Bien() {
        // Constructor requerido por JPA/Hibernate.
    }

    public String getUrlFoto() {
        return this.urlFoto;
    }
}

