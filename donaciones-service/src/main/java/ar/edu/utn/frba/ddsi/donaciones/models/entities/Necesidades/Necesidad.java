package ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estados;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Necesidad {
    private SubcategoriaBien subcategoria;
    private List<Donacion> donaciones;
    private String descripcion;

    public Necesidad(SubcategoriaBien subcategoria, String descripcion){
        this.subcategoria = subcategoria;
        this.descripcion = descripcion;
        this.donaciones = new ArrayList<>();
    }

    public void registrarDonacionAsignada(Donacion donacion) {
        this.donaciones.add(donacion);
    }

    public Integer cantidadRecibida() {
        return donaciones.stream()
                .filter(d -> d.getEstado() == Estados.ENTREGADO) // Solo se suma lo que ya llegó
                .mapToInt(Donacion::sumaCantidadBienes)
                .sum();
    }

    public abstract boolean estaSatisfecha();
}
