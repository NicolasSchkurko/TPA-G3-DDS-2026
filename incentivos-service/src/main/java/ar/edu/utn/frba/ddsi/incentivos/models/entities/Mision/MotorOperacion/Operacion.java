package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion;

import lombok.Getter;

@Getter
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
