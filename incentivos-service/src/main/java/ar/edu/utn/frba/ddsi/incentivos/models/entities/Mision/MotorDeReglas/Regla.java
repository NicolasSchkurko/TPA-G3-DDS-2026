package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.List;

public interface Regla {
    void aplicar(ImpactoDonacion donacion, Mision mision);

    void evaluarProgreso(Mision mision);

    Integer conseguirProgreso(List<ImpactoDonacion> donacionesExitosas);
}
