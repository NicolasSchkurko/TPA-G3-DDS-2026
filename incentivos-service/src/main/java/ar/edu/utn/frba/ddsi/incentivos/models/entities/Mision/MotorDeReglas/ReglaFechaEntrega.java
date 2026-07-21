package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Regla;

import java.time.YearMonth;
import java.util.List;

public class ReglaFechaEntrega implements Regla {
    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
        mision.getDonacionesExitosas().add(donacion);
    }

    @Override
    public void evaluarProgreso(Mision mision) {
        ImpactoDonacion ultimaDonacion = mision.getDonacionesExitosas().getLast();
        YearMonth ultimoMes = YearMonth.from(ultimaDonacion.getFechaEntrega());

        // Si pasó 1 mes desde la ultima donación, la lista vuelve a 0
        if (ultimoMes.isBefore(YearMonth.now().minusMonths(1))) {
            mision.getDonacionesExitosas().clear();
        }
    }

    @Override
    public Integer conseguirProgreso(List<ImpactoDonacion> donacionesExitosas) {
        return donacionesExitosas.size();
    }
}
