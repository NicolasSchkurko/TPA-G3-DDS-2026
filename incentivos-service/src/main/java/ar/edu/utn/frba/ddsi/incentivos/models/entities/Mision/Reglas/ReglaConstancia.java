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
 * {@code unidadTiempo}". No tiene setters.
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
