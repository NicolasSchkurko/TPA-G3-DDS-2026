package ar.edu.utn.frba.ddsi.incentivos.exceptions;

/**
 * El pedido es válido pero no se puede aplicar por el estado actual de los datos: borrar algo
 * en uso o reservar una posición ocupada. Se traduce a 409.
 */
public class ConflictoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
