package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Completitud extends Mision {
    Integer cantidadObjetivo;

    public Completitud(Insignia insignia, String descripcion, Integer cantidadObjetivo) {
        super(insignia, descripcion);
        this.cantidadObjetivo = cantidadObjetivo;
    }

    @Override
    public Integer getProgresoObjetivo() {
        return this.cantidadObjetivo;
    }
// empezar desde actualizarPerfil hasta actualizar mision e insignia
//    @Override
//    public Integer calcularProgresoActual(Perfil perfil) {
//        return (int) perfil.getDonaciones().stream()
//                .map(Donacion::getCategoria)
//                .filter(Objects::nonNull)
//                .distinct()
//                .count();
//    }
//
//    @Override
//    public Boolean completarMision(Perfil perfil) {
//        return this.calcularProgresoActual(perfil) >= this.getProgresoObjetivo();    }
}
