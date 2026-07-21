package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Regla;

import java.util.List;

public class ReglaCategoria implements Regla {
    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
        boolean yaExisteCategoria = mision.getDonacionesExitosas().stream()
                .anyMatch(d -> donacion.getCategoria().equalsIgnoreCase(d.getCategoria()));

        // Si es una categoría nueva para esta misión, la donación es exitosa
        if(!yaExisteCategoria){
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
