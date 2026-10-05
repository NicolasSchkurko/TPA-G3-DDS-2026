package ar.edu.utn.frba.ddsi.incentivos.exceptions;

/**
 * El recurso pedido no existe. Se traduce a 404.
 *
 * <p>Es la excepción que usa la capa de servicios. Los controllers no lanzan
 * {@code EntityNotFoundException}: antes lo hacían unos y otros para lo mismo, y el
 * mensaje al cliente dependía de por dónde se entrara.
 */
public class InexistenteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

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
