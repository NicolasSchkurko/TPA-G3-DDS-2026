package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class MisionFactory {
    public Mision crearMision(
            String nombreMision,
            String nombreInsignia,
            ReglaConstancia constancia,
            AtributoImpacto atributo,
            Operacion operacion
    ) {
        Regla regla = new Regla(constancia, atributo, operacion);
        return new Mision(nombreMision, nombreInsignia, regla);
    }
}
