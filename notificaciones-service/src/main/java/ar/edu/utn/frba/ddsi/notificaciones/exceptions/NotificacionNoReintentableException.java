package ar.edu.utn.frba.ddsi.notificaciones.exceptions;

import java.util.UUID;

/** La notificación ya salió o está en curso; no se puede reintentar. */
public class NotificacionNoReintentableException extends RuntimeException {

    public NotificacionNoReintentableException(UUID id) {
        super("La notificación " + id + " ya fue enviada o está en curso");
    }
}
