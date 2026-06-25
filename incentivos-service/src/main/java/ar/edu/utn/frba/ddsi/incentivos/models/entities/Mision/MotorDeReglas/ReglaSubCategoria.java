package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

public class ReglaSubCategoria extends Regla{
    public ReglaSubCategoria() {
        super("reglaSubCategoria");
    }

    @Override
    public void aplicar(ImpactoDonacion donacion, Mision mision) {
        boolean yaExisteCategoria = mision.getDonacionesExitosas().stream()
                .anyMatch(d -> donacion.getSubCategoria().equalsIgnoreCase(d.getSubCategoria()));

        // Si es una categoría nueva para esta misión, la donación es exitosa
        if(!yaExisteCategoria){
            mision.getDonacionesExitosas().add(donacion);
        }
    }

    @Override
    public void evaluarProgreso(Mision mision) {
    }
}
