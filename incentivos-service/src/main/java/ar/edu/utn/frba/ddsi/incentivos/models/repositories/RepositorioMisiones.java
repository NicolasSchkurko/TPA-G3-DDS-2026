package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaCantidadBienes;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaEstado;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaFechaEntrega;

import java.util.ArrayList;
import java.util.List;

public class RepositorioMisiones {
    private final List<Mision> misiones;
    private MisionFactory factory;

    private RepositorioMisiones() {
        this.misiones = new ArrayList<>();
        inicializarMisionesBase();
    }

    private void inicializarMisionesBase() {
        Mision mision = factory.crearMision(TipoMision.RACHA);
        misiones.add(mision);
        mision = factory.crearMision(TipoMision.COMPLETITUD);
        misiones.add(mision);
        mision = factory.crearMision(TipoMision.HABIL_DONADOR);
        misiones.add(mision);
        mision = factory.crearMision(TipoMision.DONACIONES_EXITOSAS);
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
