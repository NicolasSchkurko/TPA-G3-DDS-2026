package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.ValoresDistintos;
import org.springframework.stereotype.Component;

@Component
public class OperacionFactory {
    public Operacion coincidencias(Integer progresoObjetivo, Object valorEsperado) {
        return new CantidadCoincidencias(progresoObjetivo, valorEsperado);
    }

    public Operacion valoresDistintos(Integer progresoObjetivo) {
        return new ValoresDistintos(progresoObjetivo);
    }

    public Operacion superaCantidad(Integer progresoObjetivo, Integer cantidadEsperada) {
        return new SuperaCantidad(progresoObjetivo, cantidadEsperada);
    }
}
