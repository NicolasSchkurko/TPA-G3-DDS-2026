package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import jakarta.persistence.Entity;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class BienPerecedero extends Bien {
    private LocalDate fechaVencimiento;

    public BienPerecedero(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadUtilizada, LocalDate fechaVencimiento) {
        super(descripcion, subcategoria, urlFoto, cantidad, unidadUtilizada);
        this.fechaVencimiento = fechaVencimiento;
    }

    protected BienPerecedero() {
        // Constructor requerido por JPA/Hibernate.
    }

    @Override
    public String toString() {
        return "BienPerecedero{descripcion=" + descripcion + ", subcategoria=" + subcategoria + ", urlFoto=" + urlFoto + ", cantidad=" + peso + ", unidadUtilizada=" + unidadUtilizada + ", fechaVencimiento=" + fechaVencimiento + '}';
    }
}