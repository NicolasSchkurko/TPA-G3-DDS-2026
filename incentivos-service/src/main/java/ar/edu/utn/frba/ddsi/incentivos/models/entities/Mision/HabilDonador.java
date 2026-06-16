package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;

public class HabilDonador extends Mision {
    public HabilDonador(String nombre, Insignia insignia, Integer cantidadObjetivo) {
        super(nombre, insignia, cantidadObjetivo);
    }

    public Integer getProgresoActual() {
        if(!this.getDonacionesExitosas().isEmpty()){
            return this.getDonacionesExitosas().getFirst().getCantidadBienes();
        }
        return 0;
    }

    @Override
    public void evaluarDonacion(ImpactoDonacion donacion) {
        if (donacion != null && donacion.getCantidadBienes() != null
                && donacion.getCantidadBienes() > this.getProgresoObjetivo()) {
            this.getDonacionesExitosas().add(donacion);
        }
    }
}
