package ar.edu.utn.frba.ddsi.incentivos.exceptions;

/** El recurso pedido no existe. Se traduce a 404. */
public class InexistenteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InexistenteException() {
        super();
    }

    /** El mensaje nombra el recurso que falta. Con el constructor vacío, la respuesta es genérica. */
    public InexistenteException(String mensaje) {
        super(mensaje);
    }
}
