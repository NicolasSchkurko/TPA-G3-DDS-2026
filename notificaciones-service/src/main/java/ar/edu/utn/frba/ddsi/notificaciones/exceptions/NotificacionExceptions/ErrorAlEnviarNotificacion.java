package ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions;

public class ErrorAlEnviarNotificacion extends RuntimeException {
    public ErrorAlEnviarNotificacion(String message, Throwable cause) {
        super(message,  cause);
    }
}
