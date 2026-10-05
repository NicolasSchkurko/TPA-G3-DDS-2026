package ar.edu.utn.frba.ddsi.incentivos.exceptions;

public class DatosInvalidosException extends RuntimeException {
    // Para cuando mandan datos nulos o con un formato que no se puede usar.
    // El mensaje viaja al cliente: describe qué campo está mal y qué se esperaba.

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}