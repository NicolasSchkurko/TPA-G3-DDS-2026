package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donacion {
    private PersonaDonante donante;
    private EntidadBeneficiaria entidad;
    private String descripcion;
    private List<Bien> bienes =  new ArrayList<>();
    private Estados estado;
    private SubcategoriaBien subcategoria;
    private LocalDate fechaEntrega;

    public Donacion(PersonaDonante donante, EntidadBeneficiaria entidad, String descripcion,
                         List<Bien> bienes, Estados estado, SubcategoriaBien subcategoria, LocalDate fechaEntrega){
        this.donante = donante;
        this.entidad = entidad;
        this.descripcion = descripcion;
        this.bienes = bienes;
        this.estado = estado;
        this.subcategoria = subcategoria;
        this.fechaEntrega = fechaEntrega;
    }

    public Integer sumaCantidadBienes(){
        return bienes.stream().mapToInt(Bien::getCantidad).sum();
    }

    public void agregarBien(Bien nuevoBien) {
        if (nuevoBien != null) {
            this.bienes.add(nuevoBien);
        }
    }

    public void agregarBienes(List<Bien> nuevosBienes) {
        this.bienes.addAll(nuevosBienes);
    }

}
