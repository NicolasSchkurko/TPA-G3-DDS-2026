package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;


import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public class Racha extends Mision {
    public Racha(String nombre, Insignia insignia, Integer cantidadObjetivo) {
        super(nombre, insignia, cantidadObjetivo);
    }

    @Override
    public void evaluarDonacion(ImpactoDonacion donacion) {
        if (donacion == null || donacion.getFechaEntrega() == null || !"ENTREGADA".equalsIgnoreCase(donacion.getEstado())) {
            return;
        }

        // Si es la primera donación, la agregamos y salimos
        if (this.getDonacionesExitosas().isEmpty()) {
            this.getDonacionesExitosas().add(donacion);
            return;
        }

        ImpactoDonacion ultimaDonacion = this.getDonacionesExitosas().getLast();
        YearMonth ultimoMes = YearMonth.from(ultimaDonacion.getFechaEntrega());
        YearMonth mesActual = YearMonth.from(donacion.getFechaEntrega());

        long mesesDiferencia = ChronoUnit.MONTHS.between(ultimoMes, mesActual);

        if (mesesDiferencia <= 0) {
            return;
        }
        // Si pasó más de 1 mes exacto, la racha se rompió y la lista vuelve a cero
        if (mesesDiferencia > 1) {
            this.getDonacionesExitosas().clear();
        }
        // Finalmente, si la diferencia es exactamente 1 (o si se acaba de limpiar), entra a la racha
        this.getDonacionesExitosas().add(donacion);
    }
}
