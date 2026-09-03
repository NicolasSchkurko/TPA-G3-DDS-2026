package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class BienConEstado extends Bien {
    private boolean usado;

    public BienConEstado(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadUtilizada, boolean usado) {
        super(descripcion, subcategoria, urlFoto, cantidad, unidadUtilizada);
        this.usado = usado;
    }

    protected BienConEstado() {
        // Constructor requerido por JPA/Hibernate.
    }

    public String toString() {
        return "BienConEstado{descripcion=" + descripcion + ", subcategoria=" + subcategoria + ", urlFoto=" + urlFoto + ", cantidad=" + peso + ", unidadUtilizada=" + unidadUtilizada + ", usado=" + usado + '}';
    }

}