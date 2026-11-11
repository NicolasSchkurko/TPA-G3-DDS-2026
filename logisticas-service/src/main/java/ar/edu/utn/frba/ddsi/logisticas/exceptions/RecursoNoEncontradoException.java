package ar.edu.utn.frba.ddsi.logisticas.exceptions;

/** El recurso pedido no existe. El {@code GlobalExceptionHandler} lo traduce a 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
