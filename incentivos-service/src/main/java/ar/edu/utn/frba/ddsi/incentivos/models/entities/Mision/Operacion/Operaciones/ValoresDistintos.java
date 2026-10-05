package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class ValoresDistintos extends Operacion {
    // hacer 6 donaciones de 3 categorias distintas
    //
    // Esta clase es solo la CONFIGURACION de la regla ("queremos N valores distintos").
    // Los valores que vio cada donante se guardan en su ProgresoMision, no acá: antes
    // esta entidad tenia una lista valoresDistintos que se mutaba dentro de
    // calcularProgreso, y como hay una sola fila por mision, la lista era la misma para
    // todos los donantes. Al tercero en donar, la mision ya figuraba completa para los
    // tres y se otorgaba una insignia que dos no se habian ganado.
    private Integer cantValoresDistintos;

    public ValoresDistintos(Integer progresoObjetivo,
                            Integer cantidad) {
        super(progresoObjetivo);
        this.cantValoresDistintos = cantidad;
    }

    @Override
    public Boolean estaCompleta(Integer progresoActual, ProgresoDelDonante donante) {
        return progresoActual != null
                && getProgresoObjetivo() != null
                && cantValoresDistintos != null
                && progresoActual >= getProgresoObjetivo()
                && donante.cantidadValoresObservados() >= cantValoresDistintos;
    }

    @Override
    public Boolean calcularProgreso(Object valorAtributo, ProgresoDelDonante donante) {
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
