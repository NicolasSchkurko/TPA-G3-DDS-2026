package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;

public class DonacionesExistosas extends Mision {
    public DonacionesExistosas(String nombre, Insignia insignia, Integer cantidadObjetivo) {
        super(nombre, insignia, cantidadObjetivo);
    }

    @Override
    public void evaluarDonacion(ImpactoDonacion donacion) {
        if (donacion != null && "ENTREGADA".equalsIgnoreCase(donacion.getEstado())) {
            this.getDonacionesExitosas().add(donacion);
        }
    }
}
