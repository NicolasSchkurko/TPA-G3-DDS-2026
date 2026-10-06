package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "N donaciones que superan X": la donacion cuenta solo si el atributo que se mira es
 * <b>estrictamente mayor</b> que el minimo.
 *
 * <p><b>La comparacion es estricta a proposito (punto 33).</b> Con un {@code >=} una
 * donacion de exactamente 6 bienes contaba como cumple, con lo que un donante con 6
 * daba la insignia que el enunciado reserva para los de 7 o mas: la mision semilla
 * "Habil Donador" dice "Realiza 1 donacion que <b>supera</b> 6 bienes", y en espanol
 * "supera" es "excede", no "alcanza". El nombre de la operacion, la descripcion del
 * enunciado y la comparacion tienen que decir lo mismo.
 *
 * <p><b>{@code cantidadEsperada} NO se compara en {@code esEquivalenteA}</b>, a diferencia
 * de los campos de las otras operaciones. Subir el minimo exigido no invalida lo que el
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

    /**
     * El umbral que la donacion tiene que superar, no alcanzar.
     *
     * <p>El nombre queda como "esperada" por compatibilidad con lo que ya esta en la base,
     * pero lo que significa desde el punto 33 es exclusivo: con {@code 6} cuenta desde 7.
     */
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
