package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public class ReglaFechaEntrega extends Regla {
    public ReglaFechaEntrega() {
        super("reglaFechaEntrega");
    }

    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
        // Si es la primera donación, la agregamos y salimos
        if (mision.getDonacionesExitosas().isEmpty()) {
            mision.getDonacionesExitosas().add(donacion);
        } else {
            ImpactoDonacion ultimaDonacion = mision.getDonacionesExitosas().getLast();
            YearMonth ultimoMes = YearMonth.from(ultimaDonacion.getFechaEntrega());
            YearMonth mesActual = YearMonth.from(donacion.getFechaEntrega());

            long mesesDiferencia = ChronoUnit.MONTHS.between(ultimoMes, mesActual);

            // Si pasó más de 1 mes, la lista vuelve a 0
            if (mesesDiferencia > 1) {
//aunque seria mejor un cron en vez de esta solucion, es mas flexible para calcular diferentes plazos
                mision.getDonacionesExitosas().clear();
mision.getDonacionesExitosas().add(donacion);
            } else {
                mision.getDonacionesExitosas().add(donacion);
            }
        }
    }
}
