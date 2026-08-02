package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

@Getter
@Setter
public class CantidadCoincidencias extends Operacion{
    private Object valorEsperado;

    @Override
    public Boolean calcularProgreso(
            List<ImpactoDonacion> donaciones,
            Function<ImpactoDonacion, ?> atributo
    ){

    }
}
