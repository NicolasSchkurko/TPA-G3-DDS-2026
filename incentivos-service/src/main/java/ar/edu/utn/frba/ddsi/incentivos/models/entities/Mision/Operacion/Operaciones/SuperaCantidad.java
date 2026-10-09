package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "N donaciones que superan X": la donación cuenta solo si el atributo es estrictamente mayor
 * que el mínimo. {@code cantidadEsperada} no se compara en {@code esEquivalenteA}: subir el
 * umbral no invalida lo ya acreditado.
 */
@Getter
@Entity
@NoArgsConstructor
public class SuperaCantidad extends Operacion {

    /** El umbral que la donación tiene que superar (exclusivo): con {@code 6} cuenta desde 7. */
    private Integer cantidadEsperada;

    public SuperaCantidad(Integer progresoObjetivo,
                          Integer cantidadEsperada) {
        super(progresoObjetivo);
        this.cantidadEsperada = cantidadEsperada;
    }

    @Override
    public boolean calcularProgreso(
            Object valorAtributo,
            ProgresoDelDonante donante
    ) {
        if (valorAtributo instanceof Integer valorConvertido) {
            return valorConvertido > cantidadEsperada;
        }

        return false;
    }
}
