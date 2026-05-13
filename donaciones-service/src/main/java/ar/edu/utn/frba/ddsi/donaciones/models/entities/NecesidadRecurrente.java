package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
    private Integer cantidadObjetivo;
    private Periodo plazo;

    public NecesidadRecurrente(SubcategoriaBien subcategoria, String descripcion, Integer cantidadObjetivo, Periodo plazo){
        super(subcategoria, descripcion);
        this.cantidadObjetivo = cantidadObjetivo;
        this.plazo = plazo;
    }

    @Override
    public boolean estaSatisfecha() {
        //TODO debe verificar por los diferentes plazos
        return this.cantidadRecibida() >= cantidadObjetivo;
    }
}
