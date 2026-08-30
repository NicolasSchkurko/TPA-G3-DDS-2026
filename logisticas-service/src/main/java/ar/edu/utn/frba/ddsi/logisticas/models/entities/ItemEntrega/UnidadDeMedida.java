package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnidadDeMedida {
    public static final UnidadDeMedida UNIDADES = new UnidadDeMedida("Unidades", 1.0, 0.01);
    public static final UnidadDeMedida KILOGRAMOS = new UnidadDeMedida("Kilogramos", 1.0, 0.001);
    public static final UnidadDeMedida LITROS = new UnidadDeMedida("Litros", 1.0, 0.001);

    private final String nombre;
    private final double factorPesoKgPorUnidad;
    private final double factorVolumenM3PorUnidad;

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