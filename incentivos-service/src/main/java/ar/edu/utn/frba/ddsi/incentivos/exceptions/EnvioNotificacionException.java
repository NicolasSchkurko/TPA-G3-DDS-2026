package ar.edu.utn.frba.ddsi.incentivos.exceptions;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import lombok.Getter;

/**
 * No se pudo entregar una notificación.
 *
 * <p>Lleva el mensaje que falló para poder reintentarlo. El campo es {@code transient} a
 * propósito: estas excepciones no se serializan (nadie las manda por red ni las guarda),
 * y {@link PerfilNotificacionDTO} no es serializable, así que dejarlo como campo normal
 * solo servía para que el compilador avise de un problema que no existe.
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
