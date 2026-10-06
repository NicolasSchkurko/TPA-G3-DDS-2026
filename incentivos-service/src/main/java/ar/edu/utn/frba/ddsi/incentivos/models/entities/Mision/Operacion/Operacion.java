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
 * Qué cuenta como progreso en una misión, según el tipo que el admin eligió al crearla.
 *
 * <p>Es una jerarquía deStrategy: las subclases son {@code CantidadCoincidencias},
 * {@code ValoresDistintos} y {@code SuperaCantidad}. Todas comparten el
 * {@code progresoObjetivo}; cada una agrega lo que necesita para contar.
 *
 * <p>Usa {@code SINGLE_TABLE}: las tres filas viven en la misma tabla, distinguidas por la
 * columna de discriminante. Con pocas subclases y muchos registros eso evita el JOIN de
 * una tabla por clase.
 */
@Getter
// patron strategy
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
     * Si el donante ya cumplio lo que la operacion pide.
     *
     * @param donante el avance del donante que está haciendo la misión. Las operaciones
     *                que dependen solo del contador no lo usan, pero las que tienen que
     *                recordar qué vio el donante (como {@code ValoresDistintos}) sí, y
     *                por eso va en la firma en vez de guardarse en la operación: si se
     *                guardara ahí, sería estado compartido entre todos los donantes de
     *                la misión.
     */
    public boolean estaCompleta(Integer progresoActual, ProgresoDelDonante donante) {
        return progresoActual != null
               && progresoObjetivo != null
               && progresoActual >= progresoObjetivo;
    }

    /**
     * Calcula cuántos puntos aporta esta donación al progreso.
     *
     * @param valorAtributo el valor del {@code AtributoImpacto} que la misión está
     *                      mirando, ya extraído de la donación. Qué tipo tiene depende del
     *                      atributo: un texto para {@code ESTADO}, un número para
     *                      {@code CANTIDAD_BIENES}.
     * @param donante       el avance de este donante, para las operaciones que tienen que
     *                      acordarse de lo que vieron la vez pasada.
     * @return {@code true} si esta donación cuenta para la misión. La cantidad de puntos
     *         la decide {@code ProgresoMision}, que suma de a uno; lo que se delega acá es
     *         únicamente si la donación cuenta o no.
     */
    public abstract boolean calcularProgreso(
            Object valorAtributo,
            ProgresoDelDonante donante
    );

    /**
     * Si dos operaciones piden exactamente lo mismo. Es lo que permite decidir si al
     * editar una misión el progreso acumulado sigue siendo válido (punto 15).
     *
     * <p>Se comparan el tipo de operación y el objetivo, que son las dos cosas que
     * comparten todas. Cada subclase agrega sus propios parámetros con un
     * {@code instanceof} y un {@code super}, así que agregar una operación nueva no
     * compila hasta que defina qué es "cambió de verdad" para ella.
     *
     * <p>No se comparan los ids: son de base de datos y dos objetos recién construidos
     * siempre serían distintos.
     */
    public boolean esEquivalenteA(Operacion otra) {
        return otra != null
               && getClass().equals(otra.getClass())
               && Objects.equals(progresoObjetivo, otra.progresoObjetivo);
    }
}
