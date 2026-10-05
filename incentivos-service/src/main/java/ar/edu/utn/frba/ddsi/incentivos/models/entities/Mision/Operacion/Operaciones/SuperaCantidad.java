package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "N donaciones de al menos X": la donacion cuenta solo si el atributo que se mira llega
 * al minimo.
 *
 * <p>OJO: {@code cantidadEsperada} NO se compara en {@code esEquivalenteA}, a diferencia de
 * los campos de las otras operaciones. Subir el minimo exigido no invalida lo que el
 * donante ya acredito: una donacion de 5 bienes seguia contando antes y sigue contando
 * ahora, aunque el minimo pase de 4 a 6. Si se comparara, un retoque en el umbral le
 * borraria el progreso a todos los que estaban en la mision.
 *
 * <p>No tiene setters: la configuracion no se edita, se reemplaza la operacion entera.
 */
@Getter
@Entity
@NoArgsConstructor
public class SuperaCantidad extends Operacion {

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
            return valorConvertido >= cantidadEsperada;
        }

        return false;
    }
}
