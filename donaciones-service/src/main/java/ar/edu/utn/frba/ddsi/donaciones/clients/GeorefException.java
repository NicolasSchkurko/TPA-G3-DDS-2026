package ar.edu.utn.frba.ddsi.donaciones.clients;

/**
 * Falla de infraestructura al hablar con Georef (timeout, 5xx, DNS, etc.).
 *
 * <p>Se distingue de "Georef respondió pero no encontró la dirección": esa última es un
 * problema de datos y no debe consumir intentos del cron de la misma forma.
 */
public class GeorefException extends RuntimeException {

    public GeorefException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
