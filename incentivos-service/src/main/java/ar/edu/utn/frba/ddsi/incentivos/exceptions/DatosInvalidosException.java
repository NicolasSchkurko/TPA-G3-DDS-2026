package ar.edu.utn.frba.ddsi.incentivos.exceptions;

/**
 * El pedido está mal formado: un campo obligatorio nulo o con formato inválido. Se traduce a
 * 400, también para las llamadas internas que no pasan por Bean Validation.
 */
public class DatosInvalidosException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    // El mensaje viaja al cliente y describe qué campo está mal.

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
