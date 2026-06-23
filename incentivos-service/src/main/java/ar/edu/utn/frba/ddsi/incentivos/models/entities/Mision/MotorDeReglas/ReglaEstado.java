package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

public class ReglaEstado extends Regla{
    public ReglaEstado() {
        super("reglaEstado");
    }

    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
        if("ENTREGADA".equalsIgnoreCase(donacion.getEstado())){
            mision.getDonacionesExitosas().add(donacion);
        }
    }
}
