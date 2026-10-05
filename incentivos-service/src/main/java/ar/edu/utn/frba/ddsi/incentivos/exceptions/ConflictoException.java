package ar.edu.utn.frba.ddsi.incentivos.exceptions;

/**
 * El pedido es válido pero no se puede aplicar por el estado actual de los datos.
 *
 * <p>Se usa en dos situaciones distintas:
 *
 * <ul>
 *   <li>Intentar borrar algo que todavía está en uso (punto 18): la categoría tiene
 *       donantes asignados, o la insignia de la misión ya fue obtenida.</li>
 *   <li>Intentar reservar una posición de la secuencia de categorías que ya está
 *       ocupada (punto 19).</li>
 * </ul>
 *
 * <p>Es un 409 y no un 400 porque el mismo pedido podría aplicarse bien más adelante: si
 * se libera la categoría o la posición, el mismo pedido pasa. Y no es un 500 porque el
 * servidor está bien: es una regla de negocio.
 */
public class ConflictoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
