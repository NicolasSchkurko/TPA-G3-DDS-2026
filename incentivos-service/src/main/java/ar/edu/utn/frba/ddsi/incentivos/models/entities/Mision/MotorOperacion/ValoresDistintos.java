package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ValoresDistintos extends Operacion{
    // hacer 6 donaciones de 3 categorias distintas
    private List<Object> valoresDistintos;
    private Integer cantValoresDistintos;

    public ValoresDistintos(Integer progresoObjetivo,
                            Integer cantidad) {
        super(progresoObjetivo);
        this.valoresDistintos = new ArrayList<>();
        this.cantValoresDistintos = cantidad;
    }

    @Override
    public Boolean estaCompleta(Integer progresoActual) {
        return progresoActual >= getProgresoObjetivo()
                && valoresDistintos.size() >= cantValoresDistintos;
    }

    @Override
    public Boolean calcularProgreso(
            Object valorAtributo
    ){
        if (!valoresDistintos.contains(valorAtributo)) {
            valoresDistintos.add(valorAtributo);
        }

        return true;
    }
}
