package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
//patron strategy
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NoArgsConstructor
public abstract class Operacion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idOperacion;

    private Integer progresoObjetivo;

    protected Operacion(Integer progresoObjetivo) {
        this.progresoObjetivo = progresoObjetivo;
    }

    /**
     * @param donante el avance del donante que está haciendo la misión. Las operaciones
     *                que dependen solo del contador no lo usan, pero las que tienen que
     *                recordar qué vio el donante (como {@code ValoresDistintos}) sí, y
     *                por eso va en la firma en vez de guardarse en la operación: si se
     *                guardara ahí, sería estado compartido entre todos los donantes de
     *                la misión.
     */
    public Boolean estaCompleta(Integer progresoActual, ProgresoDelDonante donante) {
        return progresoActual != null
               && progresoObjetivo != null
               && progresoActual >= progresoObjetivo;
    }

    public abstract Boolean calcularProgreso(
            Object valorAtributo,
            ProgresoDelDonante donante
    );
}
