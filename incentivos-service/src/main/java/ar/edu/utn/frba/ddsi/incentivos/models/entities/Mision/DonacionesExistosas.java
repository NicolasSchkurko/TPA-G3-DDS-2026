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
    public Boolean completarMision(Perfil perfil) {
        return perfil.getDonaciones().stream().filter(donacion -> !donacion.getEstado().equals(ENTREGADO)).size() > donacionesObjetivo;
    }
}
