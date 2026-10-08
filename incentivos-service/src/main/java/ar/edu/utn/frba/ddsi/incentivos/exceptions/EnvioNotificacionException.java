package ar.edu.utn.frba.ddsi.incentivos.exceptions;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import lombok.Getter;

/**
 * No se pudo entregar una notificación. Lleva el mensaje que falló para poder reintentarlo;
 * es {@code transient} porque la excepción no se serializa.
 */
@Getter
public class EnvioNotificacionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient PerfilNotificacionDTO mensaje;

    public EnvioNotificacionException(PerfilNotificacionDTO mensaje) {
        super("No se pudo entregar la notificación a " + mensaje);
        this.mensaje = mensaje;
    }
}
