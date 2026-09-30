package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Component
public class MisionFactory {
    private final OperacionFactory operacionFactory;

    public MisionFactory(OperacionFactory operacionFactory) {
        this.operacionFactory = operacionFactory;
    }

    public Mision crearMision(
        UUID idAdmin,
        String nombreMision,
        String descripcion,
        String nombreInsignia,
        Integer cantidadTiempo,
        String unidadTiempo,
        String atributo,
        String tipoOperacion,
        Integer progresoObjetivo,
        Integer cantidadOperacion,
        String valorEsperado
    ) {
        ReglaConstancia constancia = crearConstancia(cantidadTiempo, unidadTiempo);
        AtributoImpacto atributoImpacto = crearAtributoImpacto(atributo);
        Operacion operacion = crearOperacion(
            tipoOperacion,
            progresoObjetivo,
            cantidadOperacion,
            valorEsperado
        );

        return crearMision(
            idAdmin,
            nombreMision,
            descripcion,
            nombreInsignia,
            constancia,
            atributoImpacto,
            operacion
        );
    }

    public ReglaConstancia crearConstancia(Integer cantidadTiempo, String unidadTiempo) {
        if (cantidadTiempo == null || unidadTiempo == null || unidadTiempo.isBlank()) {
            return null;
        }
        ChronoUnit unidad = ChronoUnit.valueOf(unidadTiempo.toUpperCase(Locale.ROOT));
        return new ReglaConstancia(cantidadTiempo, unidad);
    }

    public AtributoImpacto crearAtributoImpacto(String atributo) {
        return AtributoImpacto.valueOf(atributo.toUpperCase(Locale.ROOT));
    }

    public Operacion crearOperacion(String tipoOperacion,
                                    Integer progresoObjetivo,
                                    Integer cantidad,
                                    String valor) {
        return operacionFactory.conseguirOperacion(
            tipoOperacion,
            progresoObjetivo,
            cantidad,
            valor
        );
    }

    public Mision crearMision(
        UUID idAdmin,
        String nombreMision,
        String descripcion,
        String nombreInsignia,
        ReglaConstancia constancia,
        AtributoImpacto atributo,
        Operacion operacion
    ) {
        Regla regla = new Regla(constancia, atributo, operacion);
        return new Mision(nombreMision, idAdmin, descripcion, nombreInsignia, regla);
    }
}