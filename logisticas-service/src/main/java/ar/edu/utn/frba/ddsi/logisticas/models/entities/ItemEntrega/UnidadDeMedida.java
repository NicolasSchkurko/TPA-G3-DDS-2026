package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.UUID;

@Document(collection = "unidad_medida")
@Getter
@Setter
@NoArgsConstructor(force = true)
public class UnidadDeMedida {
    public static final UnidadDeMedida UNIDADES = new UnidadDeMedida("Unidades", 1.0, 0.01);
    public static final UnidadDeMedida KILOGRAMOS = new UnidadDeMedida("Kilogramos", 1.0, 0.001);
    public static final UnidadDeMedida LITROS = new UnidadDeMedida("Litros", 1.0, 0.001);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Field(name = "id_unidad")
    private UUID idUnidad;

    /** Optimistic locking: una escritura concurrente tira OptimisticLockingFailureException en vez de pisar. */
    @Version
    private Long version;

    @Field(name = "nombre")
    private String nombre;

    @Field(name = "factor_peso_kg_por_unidad")
    private double factorPesoKgPorUnidad;

    @Field(name = "factor_volumen_m3_por_unidad")
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
