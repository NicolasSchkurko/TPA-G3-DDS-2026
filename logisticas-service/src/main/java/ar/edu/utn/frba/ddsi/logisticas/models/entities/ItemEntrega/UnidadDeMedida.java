package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "unidad_medida")
@Getter
@Setter
@NoArgsConstructor(force = true)
public class UnidadDeMedida {
    public static final UnidadDeMedida UNIDADES = new UnidadDeMedida("Unidades", 1.0, 0.01);
    public static final UnidadDeMedida KILOGRAMOS = new UnidadDeMedida("Kilogramos", 1.0, 0.001);
    public static final UnidadDeMedida LITROS = new UnidadDeMedida("Litros", 1.0, 0.001);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_unidad", nullable = false, updatable = false)
    private UUID idUnidad;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "factor_peso_kg_por_unidad", nullable = false)
    private double factorPesoKgPorUnidad;

    @Column(name = "factor_volumen_m3_por_unidad", nullable = false)
    private double factorVolumenM3PorUnidad;

    public UnidadDeMedida(String nombre, double factorPesoKgPorUnidad, double factorVolumenM3PorUnidad) {
        this.nombre = nombre;
        this.factorPesoKgPorUnidad = factorPesoKgPorUnidad;
        this.factorVolumenM3PorUnidad = factorVolumenM3PorUnidad;
    }

    public double calcularPesoKg(Integer cantidad) {
        return cantidad * factorPesoKgPorUnidad;
    }

    public double calcularVolumenM3(Integer cantidad) {
        return cantidad * factorVolumenM3PorUnidad;
    }
}