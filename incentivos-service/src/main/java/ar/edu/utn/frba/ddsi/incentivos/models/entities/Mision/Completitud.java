package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Completitud extends Mision {

    public Completitud(String nombre, Insignia insignia, Integer cantidadObjetivo) {
        super(nombre, insignia, cantidadObjetivo);
    }

    @Override
    public void evaluarDonacion(ImpactoDonacion donacion) {
        if (donacion == null || donacion.getCategoria() == null) {
            return;
        }
        boolean yaExisteCategoria = this.getDonacionesExitosas().stream()
                .anyMatch(d -> donacion.getCategoria().equalsIgnoreCase(d.getCategoria()));

        // Si es una categoría nueva para esta misión, la donación es exitosa
        if (!yaExisteCategoria) {
            this.getDonacionesExitosas().add(donacion);
        }
    }
}
