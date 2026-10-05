package ar.edu.utn.frba.ddsi.incentivos.exceptions;

/**
 * No existe la categoría base "Colaborador", que es la primera del programa y de la que
 * cuelga el resto de la secuencia.
 *
 * <p>Existe aparte de {@link InexistenteException} porque no es un 404 cualquiera: si
 * falta, la secuencia de posiciones está rota y hay que reindexar, no simplemente
 * responder que el recurso no está.
 */
public class CategoriaBaseInexistenteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CategoriaBaseInexistenteException(String message) {
        super(message);
    }
}
