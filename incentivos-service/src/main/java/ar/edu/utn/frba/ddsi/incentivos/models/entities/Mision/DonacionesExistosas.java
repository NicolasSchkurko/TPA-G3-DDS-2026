package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.List;

public class DonacionesExistosas extends Mision {
    Integer donacionesObjetivo;

    public DonacionesExistosas(Insignia insignia, String descripcion, Integer donacionesObjetivo) {
        super(insignia, descripcion);
        this.donacionesObjetivo = donacionesObjetivo;
    }

    @Override
    public Integer getProgresoObjetivo() {
        return this.donacionesObjetivo;
    }

//    @Override
//    public Integer calcularProgresoActual(Perfil perfil) {
//        return (int) perfil.getDonaciones().stream()
//                .filter(donacion -> "ENTREGADA".equalsIgnoreCase(donacion.getEstado()))
//                .count();
//    }
//
//    @Override
//    public Boolean completarMision(Perfil perfil) {
//        return this.calcularProgresoActual(perfil) >= this.getProgresoObjetivo();
//    }
}
