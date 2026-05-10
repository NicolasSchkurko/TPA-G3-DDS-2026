package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaJuridica;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


public class Donacion {
    @Getter
    @Setter
    private PersonaDonante donante;
    private EntidadBeneficiaria entidad;
    private List<Bien> bienes;
    private Estados estado;
    private SubcategoriaBien subcategoria;

    public Donacion(PersonaDonante donante, EntidadBeneficiaria entidad, String descripcion,
                         List<Bien> bienes, Estados estado, SubcategoriaBien subcategoria){
        this.donante = donante;
        this.entidad = entidad;
        this.descripcion = descripcion;
        this.bienes = bienes;
        this.estado = estado;
        this.subcategoria = subcategoria;
    }

    public Integer SumaCantidadBienes(){
        Integer cantidad = 0;
        bienes.forEach(bien -> {
            cantidad += bien.cantidad;
        });
    }


}
