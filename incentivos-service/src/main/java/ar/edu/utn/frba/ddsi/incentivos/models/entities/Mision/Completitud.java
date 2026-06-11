package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.List;
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
    public Boolean completarMision(Perfil perfil) {
        return perfil.getDonaciones().stream().map(donacion -> donacion.getCategoria()).distinct().count() > cantidadObjetivo;
    }
}
