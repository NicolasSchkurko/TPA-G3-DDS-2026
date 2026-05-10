package ar.edu.utn.frba.ddsi.donaciones.models.entities;

public class NecesidadRecurrente extends Necesidad {
    private Integer cantidadRecibida;
    private Integer cantidadObjetivo;
    private Periodo plazo;

    public NecesidadRecurrente(SubcategoriaBien subcategoria, String descripcion, Integer cantidadRecibida, Integer cantidadObjetivo, Periodo plazo){
        super(subcategoria, descripcion);
        this.cantidadRecibida = cantidadRecibida;
        this.cantidadObjetivo = cantidadObjetivo;
        this.plazo = plazo;
    }
}
