package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * La condición de tiempo de una regla: "una donación cada {@code cantidad}
 * {@code unidadTiempo}".
 *
 * <p>{@code ChronoUnit} permite minutos, horas, días, semanas, meses y años. En la
 * práctica solo meses y días son útiles, porque {@code ImpactoDonacion} guarda la fecha
 * como {@code LocalDateTime} y la unidad tiene que ser una que el servicio de donaciones
 * pueda mandar.
 *
 * <p>No tiene setters: la constancia se define al crear la misión.
 */
@Getter
@Entity
@NoArgsConstructor
public class ReglaConstancia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    private ChronoUnit unidadTiempo;

    public ReglaConstancia(
            Integer cantidad,
            ChronoUnit unidadTiempo
    ) {
        this.cantidad = cantidad;
        this.unidadTiempo = unidadTiempo;
    }
}
