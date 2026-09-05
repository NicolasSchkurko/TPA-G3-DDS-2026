package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
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
    private Integer cantidadEsperada;

    public SuperaCantidad(Integer progresoObjetivo,
                          Integer cantidadEsperada) {
        super(progresoObjetivo);
        this.cantidadEsperada = cantidadEsperada;
    }

    @Override
    public Boolean calcularProgreso(
            Object valorAtributo
    ){
        if (valorAtributo instanceof Integer valorConvertido) {
            return valorConvertido >= cantidadEsperada;
        }

        return false;
    }
}
