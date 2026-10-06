package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.Entity;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "N donaciones de M valores distintos": el donante tiene que haber donado en al menos
 * {@code cantValoresDistintos} categorías (o lo que se esté mirando) diferentes.
 *
 * <p>Esta clase es solo la CONFIGURACIÓN de la regla. Los valores que vio cada donante se
 * guardan en su {@code ProgresoMision}, no acá: antes esta entidad tenía una lista
 * {@code valoresDistintos} que se mutaba dentro de {@code calcularProgreso}, y como hay
 * una sola fila por misión, la lista era la misma para todos los donantes. Al tercero en
 * donar, la misión ya figuraba completa para los tres y se otorgaba una insignia que dos
 * no se habían ganado.
 *
 * <p>No tiene setters: la configuración no se edita, se reemplaza la operación entera.
 */
@Getter
@Entity
@NoArgsConstructor
public class ValoresDistintos extends Operacion {

    private Integer cantValoresDistintos;

    /**
     * Acá sí se compara {@code cantValoresDistintos}, al revés que en
     * {@code SuperaCantidad}: cambiar cuántos valores distintos se piden cambia lo que el
     * donante tiene que haber hecho, así que lo que acumuló deja de servir. Bajarlo es
     * un caso particular: el donante no pierde lo que ya vio, solo que ahora puede
     * completar antes. Por eso el reinicio se hace igual y el avance real no se borra.
     */
    @Override
    public boolean esEquivalenteA(Operacion otra) {
        return super.esEquivalenteA(otra)
               && otra instanceof ValoresDistintos otraDistintos
               && Objects.equals(cantValoresDistintos, otraDistintos.cantValoresDistintos);
    }

    public ValoresDistintos(Integer progresoObjetivo,
                            Integer cantidad) {
        super(progresoObjetivo);
        this.cantValoresDistintos = cantidad;
    }

    @Override
    public boolean estaCompleta(Integer progresoActual, ProgresoDelDonante donante) {
        return progresoActual != null
                && getProgresoObjetivo() != null
                && cantValoresDistintos != null
                && progresoActual >= getProgresoObjetivo()
                && donante.cantidadValoresObservados() >= cantValoresDistintos;
    }

    @Override
    public boolean calcularProgreso(Object valorAtributo, ProgresoDelDonante donante) {
        // Una donacion sin el atributo que mira la regla no aporta nada: no cuenta como
        // donacion para el objetivo ni como valor distinto. Antes se guardaba el null en
        // la lista y eso inflaba el conteo de valores distintos con un elemento vacio.
        if (valorAtributo == null) {
            return false;
        }

        donante.registrarValorObservado(String.valueOf(valorAtributo));

        return true;
    }
}
