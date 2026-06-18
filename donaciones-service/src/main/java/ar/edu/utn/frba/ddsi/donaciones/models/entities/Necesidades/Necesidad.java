package ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Necesidad {
    private UUID id = UUID.randomUUID(); // Identificador único autogenerado
    SubcategoriaBien subcategoria;
    List<Donacion> donaciones;
    String descripcion;
    Integer cantidadObjetivo;

    public Necesidad(SubcategoriaBien subcategoria, String descripcion, Integer cantidadObjetivo) {
        this.subcategoria = subcategoria;
        this.descripcion = descripcion;
        this.cantidadObjetivo = cantidadObjetivo;
        this.donaciones = new ArrayList<>();
    }

    public void registrarDonacionAsignada(Donacion donacion) {
        this.donaciones.add(donacion);
    }

    public Integer cantidadRecibida() {
        return donaciones.stream()
                         .filter(d -> d.getEstado() == Estado.ENTREGADO) // Solo se suma lo que ya llegó
                         .mapToInt(Donacion::sumaCantidadBienes)
                         .sum();
    }

    public abstract boolean estaSatisfecha();

    public boolean esCompatibleCon(Donacion donacion) {
        return !this.estaSatisfecha() && this.subcategoria.equals(donacion.getSubcategoria());
    }
}