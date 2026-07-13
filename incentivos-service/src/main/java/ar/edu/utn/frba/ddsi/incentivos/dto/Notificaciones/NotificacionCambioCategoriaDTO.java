package ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones;

import ar.edu.utn.frba.ddsi.incentivos.dto.MedioContactoDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import lombok.Getter;

@Getter
public class NotificacionCambioCategoriaDTO {
    private Perfil perfil;

    private Perfil perfilAnterior;

    private MedioContactoDTO medioContacto;

    public NotificacionCambioCategoriaDTO(Perfil perfil, Perfil perfilAnterior, MedioContactoDTO medioContacto) {
        this.perfil = perfil;
        this.perfilAnterior = perfilAnterior;
        this.medioContacto = medioContacto;
    }
}
