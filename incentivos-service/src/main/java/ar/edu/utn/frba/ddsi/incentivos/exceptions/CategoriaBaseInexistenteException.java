package ar.edu.utn.frba.ddsi.incentivos.exceptions;

/**
 * No existe la categoría base, la primera del programa. Es un problema de datos, no un 404
 * común: la secuencia está rota.
 */
public class CategoriaBaseInexistenteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CategoriaBaseInexistenteException(String message) {
        super(message);
    }
}
