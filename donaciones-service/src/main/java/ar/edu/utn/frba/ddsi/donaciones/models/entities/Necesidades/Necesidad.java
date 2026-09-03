package ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Necesidad {

    @Id
    private UUID id = UUID.randomUUID(); // Identificador único autogenerado

    @ManyToOne
    @JoinColumn(name = "subcategoria_id")
    SubcategoriaBien subcategoria;

    // Donacion todavía no es una entidad JPA (persistencia de ese dominio queda para otro paso/servicio).
    @Transient
    List<Donacion> donaciones = new ArrayList<>();

    String descripcion;
    Integer cantidadObjetivo;

    public Necesidad(SubcategoriaBien subcategoria, String descripcion, Integer cantidadObjetivo) {
        this.subcategoria = subcategoria;
        this.descripcion = descripcion;
        this.cantidadObjetivo = cantidadObjetivo;
        this.donaciones = new ArrayList<>();
    }

    protected Necesidad() {
        // Constructor requerido por JPA/Hibernate.
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
        return !this.estaSatisfecha() && this.subcategoria.getNombre().equals(donacion.getSubcategoria().getNombre());
    }
}