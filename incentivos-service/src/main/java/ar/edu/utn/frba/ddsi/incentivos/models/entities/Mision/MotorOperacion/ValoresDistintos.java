package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
@Setter
public class ValoresDistintos extends Operacion{
    // hacer 6 donaciones de 3 categorias distintas
    private List<Object> valoresDistintos;

    public ValoresDistintos(Integer progresoObjetivo) {
        super(progresoObjetivo);
        this.valoresDistintos = new ArrayList<>();
    }

    @Override
    public Boolean calcularProgreso(
            Object valorAtributo
    ){
        return !valoresDistintos.contains(valorAtributo);
    }
}
