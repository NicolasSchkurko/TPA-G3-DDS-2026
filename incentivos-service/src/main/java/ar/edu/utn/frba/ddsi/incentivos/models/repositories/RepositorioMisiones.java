package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import org.springframework.stereotype.Repository;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RepositorioMisiones {
    private final List<Mision> misiones;
    private final MisionFactory misionFactory;
    private final OperacionFactory operacionFactory;

    public RepositorioMisiones(MisionFactory factory,
                               OperacionFactory operacionFactory) {
        this.misionFactory = factory;
        this.operacionFactory = operacionFactory;
        this.misiones = new ArrayList<>();
        inicializarMisionesBase();
    }

    private void inicializarMisionesBase() {
        Operacion operacion = operacionFactory.coincidencias(1,
                "ENTREGADA");
        Mision mision = misionFactory.crearMision("Racha",
                "Usuario Constante",
                new ReglaConstancia(5, ChronoUnit.MONTHS),
                ImpactoDonacion::getEstado,
                operacion);
        misiones.add(mision);

        operacion = operacionFactory.valoresDistintos(5, 3);
        mision = misionFactory.crearMision("Completitud",
                "Usuario Variado",
                null,
                ImpactoDonacion::getCategoria,
                operacion);
        misiones.add(mision);

        operacion = operacionFactory.superaCantidad(1, 3);
        mision = misionFactory.crearMision("Habil Donador",
                "Usuario Generoso",
                null,
                ImpactoDonacion::getCantidadBienes,
                operacion);
        misiones.add(mision);

        operacion = operacionFactory.coincidencias(6, "ENTREGADO");
        mision = misionFactory.crearMision("Donaciones Exitosas",
                "Usuario Exitoso",
                null,
                ImpactoDonacion::getEstado,
                operacion);
        misiones.add(mision);
    }

//TODO habra tmb actualizar mision, guardar mision y eliminar

    public Mision buscarPorNombre(String nombre) {
        if (nombre == null) return null;
        return this.misiones.stream()
                .filter(m -> m.getNombreMision().equals(nombre))
                .findFirst()
                .orElse(null);
    }

    public List<Mision> obtenerTodas() {
        return new ArrayList<>(this.misiones);
    }
}
