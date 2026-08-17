package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import org.springframework.stereotype.Component;

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
