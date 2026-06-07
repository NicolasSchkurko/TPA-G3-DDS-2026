package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.List;

public class DonacionesExistosas extends Mision {
    Integer donacionesObjetivo;

    public DonacionesExistosas(Insignia insignia, Integer donacionesObjetivo) {
        super(insignia);
        this.donacionesObjetivo = donacionesObjetivo;
    }

    @Override
    public Boolean completarMision(Perfil perfil) {
        return null;//TODO
    }
}
