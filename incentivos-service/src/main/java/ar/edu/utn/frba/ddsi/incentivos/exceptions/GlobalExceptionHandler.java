package ar.edu.utn.frba.ddsi.incentivos.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> manejarAccesoDenegado(
            SecurityException exception) {
        return respuesta(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InexistenteException.class)
    public ResponseEntity<Map<String, String>> manejarInexistente(
            InexistenteException exception) {
        return respuesta(HttpStatus.NOT_FOUND, "El recurso solicitado no existe");
    }

    @ExceptionHandler(PerfilExistenteException.class)
    public ResponseEntity<Map<String, String>> manejarPerfilExistente(
            PerfilExistenteException exception) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<Map<String, String>> manejarDatosInvalidos(
            DatosInvalidosException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(
            IllegalArgumentException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, String>> manejarHeaderFaltante(
            MissingRequestHeaderException exception) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "Falta el header requerido: " + exception.getHeaderName());
    }

    @ExceptionHandler(CategoriaBaseInexistenteException.class)
    public ResponseEntity<Map<String, String>> manejarConfiguracionInvalida(
            CategoriaBaseInexistenteException exception) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> respuesta(
            HttpStatus status,
            String mensaje
    ) {
        return ResponseEntity.status(status)
                .body(Map.of("error", mensaje == null ? status.getReasonPhrase() : mensaje));
    }
}
