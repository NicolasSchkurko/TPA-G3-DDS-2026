package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

public class ReglaCantidadBienes extends Regla {
    public ReglaCantidadBienes() {
        super("reglaCantidadBienes");
    }

    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
        mision.getDonacionesExitosas().add(donacion);
    }

    @Override
    public void evaluarProgreso(Mision mision) {
    }
}
