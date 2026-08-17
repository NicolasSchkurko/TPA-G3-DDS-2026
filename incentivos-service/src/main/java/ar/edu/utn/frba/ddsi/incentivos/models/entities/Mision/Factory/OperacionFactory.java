package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.*;
import org.springframework.stereotype.Component;

@Component
public class OperacionFactory {
    public Operacion conseguirOperacion(TipoOperacion tipo, Integer progresoObjetivo,
                                        Integer cantidad, Object valor){
        return switch (tipo) {
            case COINCIDENCIAS ->
                    new CantidadCoincidencias(progresoObjetivo, valor);
            case SUPERA_CANTIDAD ->
                    new SuperaCantidad(progresoObjetivo, cantidad);
            case VALORES_DISTINTOS ->
                    new ValoresDistintos(progresoObjetivo, cantidad);
        };
    }
}
