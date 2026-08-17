package ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones;

import ar.edu.utn.frba.ddsi.incentivos.dto.MedioContactoDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import lombok.Getter;

@Getter
public class NotificacionMisionDTO {

    private Perfil perfil;

    private MedioContactoDTO medioContacto;

    public NotificacionMisionDTO(Perfil perfil, MedioContactoDTO medioContacto) {
        this.perfil = perfil;
        this.medioContacto = medioContacto;
    }
}
