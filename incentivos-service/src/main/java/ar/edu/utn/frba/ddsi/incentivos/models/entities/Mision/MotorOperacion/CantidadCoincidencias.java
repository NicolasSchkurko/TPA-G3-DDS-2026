package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion;

import lombok.Getter;
import lombok.Setter;

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
