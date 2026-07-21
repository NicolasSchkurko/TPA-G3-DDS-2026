package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Regla;

import java.util.List;

public class ReglaCantidadBienes implements Regla {
    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
        mision.getDonacionesExitosas().add(donacion);
    }

    @Override
    public void evaluarProgreso(Mision mision) {
    }

    @Override
    public Integer conseguirProgreso(List<ImpactoDonacion> donacionesExitosas) {
        return donacionesExitosas.stream().mapToInt(ImpactoDonacion::getCantidadBienes).sum();
    }
}
