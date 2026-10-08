package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.Entity;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "N donaciones de M valores distintos". Es solo la configuración: los valores que vio cada
 * donante se guardan en su {@code ProgresoMision}, no acá. No tiene setters.
 */
@Getter
@Entity
@NoArgsConstructor
public class ValoresDistintos extends Operacion {

    private Integer cantValoresDistintos;

    /**
     * Acá sí se compara {@code cantValoresDistintos}: cambiarlo cambia lo que el donante
     * tiene que haber hecho.
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
        // Una donación sin el atributo no aporta nada: no cuenta ni como donación ni como valor.
        if (valorAtributo == null) {
            return false;
        }

        donante.registrarValorObservado(String.valueOf(valorAtributo));

        return true;
    }
}
