package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.List;

public class Racha extends Mision {
    Integer mesesObjetivo;

    public Racha(Insignia insignia, Integer mesesObjetivo) {
        super(insignia);
        this.mesesObjetivo = mesesObjetivo;
    }

    @Override
    public Boolean completarMision(Perfil perfil) {
        return null; //TODO
    }
}
