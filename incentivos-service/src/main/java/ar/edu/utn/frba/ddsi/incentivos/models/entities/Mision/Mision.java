package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public abstract class Mision {
    Insignia insignia;
    String descripcion;

    public Mision (Insignia insignia, String descripcion){
        this.insignia = insignia;
        this.descripcion = descripcion;
    }

    public abstract Boolean completarMision(Perfil perfil);
}
