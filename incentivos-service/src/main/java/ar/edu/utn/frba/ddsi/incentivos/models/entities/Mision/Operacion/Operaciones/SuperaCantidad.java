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
public class SuperaCantidad extends Operacion {
    //hacer 5 donaciones de al menos 4 bienes cada una
    //
    // OJO: este campo NO se compara en esEquivalenteA, a diferencia de los de las otras
    // operaciones. Subir el minimo exigido no invalida lo que el donante ya acredito: una
    // donacion de 5 bienes seguia contando antes y sigue contando ahora, aunque el minimo
    // pase de 4 a 6. Si se comparara, un retoque en el umbral le borraria el progreso a
    // todos los que estaban en la mision.
    private Integer cantidadEsperada;

    public SuperaCantidad(Integer progresoObjetivo,
                          Integer cantidadEsperada) {
        super(progresoObjetivo);
        this.cantidadEsperada = cantidadEsperada;
    }

    @Override
    public Boolean calcularProgreso(
            Object valorAtributo,
            ProgresoDelDonante donante
    ){
        if (valorAtributo instanceof Integer valorConvertido) {
            return valorConvertido >= cantidadEsperada;
        }

        return false;
    }
}
