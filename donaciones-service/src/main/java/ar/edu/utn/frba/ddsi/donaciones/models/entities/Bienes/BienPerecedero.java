package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienPerecedero extends Bien {
    private LocalDate fechaVencimiento;

    public BienPerecedero(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadUtilizada, LocalDate fechaVencimiento) {
        super(descripcion, subcategoria, urlFoto, cantidad, unidadUtilizada);
        this.fechaVencimiento = fechaVencimiento;
    }

    @Override
    public String toString() {
        return "BienPerecedero{descripcion=" + descripcion + ", subcategoria=" + subcategoria + ", urlFoto=" + urlFoto + ", cantidad=" + cantidad + ", unidadUtilizada=" + unidadUtilizada + ", fechaVencimiento=" + fechaVencimiento + '}';
    }
}