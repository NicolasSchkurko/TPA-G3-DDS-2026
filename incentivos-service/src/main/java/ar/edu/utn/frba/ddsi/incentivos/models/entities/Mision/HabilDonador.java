package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.List;

public class HabilDonador extends Mision {
    Integer cantidadBienesObjetivo;

    public HabilDonador(Insignia insignia, String descripcion, Integer cantidadBienesObjetivo) {
        super(insignia, descripcion);
        this.cantidadBienesObjetivo = cantidadBienesObjetivo;
    }

    @Override
    public Integer getProgresoObjetivo() {
        return this.cantidadBienesObjetivo;
    }

//    @Override
//    public Integer calcularProgresoActual(Perfil perfil) {
//        return perfil.getDonaciones().stream()
//                .mapToInt(Donacion::getCantidadBienes)
//                .max()
//                .orElse(0); // Si no tiene donaciones, el progreso es 0
//    }
//
//    @Override
//    public Boolean completarMision(Perfil perfil) {
//        return this.calcularProgresoActual(perfil) > this.getProgresoObjetivo();
//    }
}
