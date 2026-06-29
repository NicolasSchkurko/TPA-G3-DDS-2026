package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.List;

public class ReglaEstado implements Regla{
    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
        if("ENTREGADA".equalsIgnoreCase(donacion.getEstado())){
            mision.getDonacionesExitosas().add(donacion);
        }
    }

    @Override
    public void evaluarProgreso(Mision mision) {
    }

    @Override
    public Integer conseguirProgreso(List<ImpactoDonacion> donacionesExitosas) {
        return donacionesExitosas.size();
    }
}
