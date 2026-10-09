package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Qué cuenta como progreso en una misión, según el tipo elegido al crearla. Jerarquía de
 * Strategy ({@code CantidadCoincidencias}, {@code ValoresDistintos}, {@code SuperaCantidad})
 * mapeada con {@code SINGLE_TABLE}.
 */
@Getter
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
     * Si el donante ya cumplió lo que la operación pide.
     *
     * @param donante el avance del donante. Va en la firma, no guardado en la operación,
     *                para no compartir estado entre donantes.
     */
    public boolean estaCompleta(Integer progresoActual, ProgresoDelDonante donante) {
        return progresoActual != null
               && progresoObjetivo != null
               && progresoActual >= progresoObjetivo;
    }

    /**
     * Si esta donación cuenta para el progreso. La cantidad de puntos la suma
     * {@code ProgresoMision}.
     *
     * @param valorAtributo el valor del atributo que mira la misión, ya extraído.
     * @param donante       el avance de este donante.
     * @return {@code true} si la donación cuenta para la misión.
     */
    public abstract boolean calcularProgreso(
            Object valorAtributo,
            ProgresoDelDonante donante
    );

    /**
     * Si dos operaciones piden exactamente lo mismo: decide si al editar una misión el
     * progreso acumulado sigue siendo válido. Compara tipo y objetivo; cada subclase agrega
     * sus parámetros.
     */
    public boolean esEquivalenteA(Operacion otra) {
        return otra != null
               && getClass().equals(otra.getClass())
               && Objects.equals(progresoObjetivo, otra.progresoObjetivo);
    }
}
