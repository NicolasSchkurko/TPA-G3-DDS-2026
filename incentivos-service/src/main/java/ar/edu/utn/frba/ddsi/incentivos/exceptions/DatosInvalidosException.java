package ar.edu.utn.frba.ddsi.incentivos.exceptions;

/**
 * El pedido está mal formado: un campo obligatorio viene nulo, o tiene un formato que no
 * se puede usar. Se traduce a 400.
 *
 * <p>Cubre también lo que Bean Validation ya rechaza por su cuenta: si el service se llama
 * desde código y no desde HTTP, las anotaciones no se evaluan y sin esta excepción un null
 * terminaba en NullPointerException, o sea en 500 en vez de 400.
 */
public class DatosInvalidosException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    // Para cuando mandan datos nulos o con un formato que no se puede usar.
    // El mensaje viaja al cliente: describe qué campo está mal y qué se esperaba.

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
