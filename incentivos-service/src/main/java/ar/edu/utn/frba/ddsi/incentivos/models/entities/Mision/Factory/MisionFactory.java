package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Component
public class MisionFactory {
    private final OperacionFactory operacionFactory;

    public MisionFactory(OperacionFactory operacionFactory){
        this.operacionFactory = operacionFactory;
    }

    public ReglaConstancia crearConstancia(Integer cantidadTiempo, String unidadTiempo){
        ChronoUnit unidad = ChronoUnit.valueOf(unidadTiempo.toUpperCase(Locale.ROOT));
        return new ReglaConstancia(cantidadTiempo, unidad);
    }

    public AtributoImpacto crearAtributoImpacto(String atributo){
        return AtributoImpacto.valueOf(atributo.toUpperCase(Locale.ROOT));
    }

    public Operacion crearOperacion(String tipoOperacion,
                                    Integer progresoObjetivo,
                                    Integer cantidad, String valor){
        return operacionFactory.conseguirOperacion(
                tipoOperacion, progresoObjetivo,
                cantidad, valor
        );
    }

    public Mision crearMision(
            String nombreMision,
            String descripcion,
            String nombreInsignia,
            ReglaConstancia constancia,
            AtributoImpacto atributo,
            Operacion operacion
    ) {
        Regla regla = new Regla(constancia, atributo, operacion);
        return new Mision(nombreMision, descripcion, nombreInsignia, regla);
    }
}
