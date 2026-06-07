package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.List;

public class Completitud extends Mision {
    Integer cantidadObjetivo;

    public Completitud(Insignia insignia, Integer cantidadObjetivo) {
        super(insignia);
        this.cantidadObjetivo = cantidadObjetivo;
    }

    @Override
    public Boolean completarMision(Perfil perfil) {
        return null;//TODO
    }
}
