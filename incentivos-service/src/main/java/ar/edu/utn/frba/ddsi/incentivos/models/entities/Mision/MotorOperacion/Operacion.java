package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import java.util.List;
import java.util.function.Function;

//patron strategy
public abstract class Operacion {
    private final Integer progresoObjetivo;

    protected Operacion(Integer progresoObjetivo) {
        this.progresoObjetivo = progresoObjetivo;
    }

    public Boolean estaCompleta(Integer progresoActual) {
        return progresoActual >= progresoObjetivo;
    }

    public abstract Boolean calcularProgreso(
            Object valorAtributo
    );
}
