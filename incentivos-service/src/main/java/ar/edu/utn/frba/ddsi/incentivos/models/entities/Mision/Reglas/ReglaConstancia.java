package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * La condición de tiempo de una regla: "una donación cada {@code cantidad}
 * {@code unidadTiempo}".
 *
 * <p>{@link UnidadTiempo} limita las unidades guardadas a las que acepta el dominio, sin
 * depender de las constantes que exponga {@code java.time.temporal.ChronoUnit}.
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
    private UnidadTiempo unidadTiempo;

    public ReglaConstancia(
            Integer cantidad,
            UnidadTiempo unidadTiempo
    ) {
        this.cantidad = cantidad;
        this.unidadTiempo = unidadTiempo;
    }
}
