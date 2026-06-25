package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

public class ReglaEntidadBeneficiaria extends Regla {
    public ReglaEntidadBeneficiaria() {
        super("reglaEntidadBeneficiaria");
    }

    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
    }

    @Override
    public void evaluarProgreso(Mision mision) {
    }
}
