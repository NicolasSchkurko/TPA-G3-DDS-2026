package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienPerecedero extends Bien {
    private Date fechaVencimiento;

    public BienPerecedero(String descripcion, SubcategoriaBien subcategoria, String foto, Integer cantidad, UnidadDeMedida unidadautilizada, Date fechaVencimiento) {
        super(descripcion, subcategoria, foto, cantidad, unidadautilizada);
        this.fechaVencimiento = fechaVencimiento;
    }
}