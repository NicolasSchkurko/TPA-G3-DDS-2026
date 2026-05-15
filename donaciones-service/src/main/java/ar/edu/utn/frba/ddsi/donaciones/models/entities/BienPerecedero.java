package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienPerecedero extends Bien {
    private LocalDate fechaVencimiento;

    public BienPerecedero(String descripcion, SubcategoriaBien subcategoria, String urlFoto, Integer cantidad, UnidadDeMedida unidadautilizada, LocalDate fechaVencimiento) {
        super(descripcion, subcategoria, urlFoto, cantidad, unidadautilizada);
        this.fechaVencimiento = fechaVencimiento;
    }
}