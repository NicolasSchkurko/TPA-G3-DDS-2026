package ar.edu.utn.frba.ddsi.incentivos.exceptions;

public class InexistenteException extends RuntimeException {

    public InexistenteException() {
        super();
    }

    /**
     * El mensaje nombra el recurso que falta, para que el cliente sepa qué pedir.
     * Si se usa el constructor vacío, la respuesta es genérica.
     */
    public InexistenteException(String mensaje) {
        super(mensaje);
    }
}