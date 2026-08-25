package ar.edu.utn.frba.ddsi.incentivos.exceptions;


import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import lombok.Getter;

@Getter
public class EnvioNotificacionException extends RuntimeException{
    private final PerfilNotificacionDTO mensaje;

    public EnvioNotificacionException(PerfilNotificacionDTO mensaje) {
        this.mensaje = mensaje;
    }

}
