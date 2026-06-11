package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
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

    public MisionDTO toDTO() {
        MisionDTO dto = new MisionDTO();
        dto.setNombreMision(this.descripcion);
        if (this.insignia != null) {
            dto.setInsigniaObjetivo(this.insignia.toDTO());
        }
        dto.setProgresoActual(null);
        dto.setProgresoObjetivo(null);
        return dto;
    }

    public abstract Boolean completarMision(Perfil perfil);
}
