package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

@Getter
@Setter
public class CantidadCoincidencias extends Operacion{
    //5 donaciones "ENTREGADAS"
    private Object valorEsperado;

    public CantidadCoincidencias(Integer progresoObjetivo,
                                 Object valorEsperado) {
        super(progresoObjetivo);
        this.valorEsperado = valorEsperado;
    }

    @Override
    public Boolean calcularProgreso(
            Object valorAtributo
    ){
        return valorAtributo.equals(valorEsperado);
    }
}
