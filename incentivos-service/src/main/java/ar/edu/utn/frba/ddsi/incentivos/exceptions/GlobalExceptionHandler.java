package ar.edu.utn.frba.ddsi.incentivos.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashMap;
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
        String mensaje = exception.getMessage() == null
                ? "El recurso solicitado no existe"
                : exception.getMessage();
        return respuesta(HttpStatus.NOT_FOUND, mensaje);
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

    /**
     * Violaciones de las anotaciones de Bean Validation sobre los DTOs de entrada.
     * Devuelve el detalle por campo para que el cliente sepa qué corregir.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
            MethodArgumentNotValidException exception) {
        Map<String, String> errores = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        exception.getBindingResult().getGlobalErrors().forEach(error ->
                errores.putIfAbsent(error.getObjectName(), error.getDefaultMessage())
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Datos de entrada inválidos");
        body.put("campos", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * JSON mal formado o un tipo que no se puede convertir (por ejemplo un
     * {@code YearMonth} con un formato inválido en el body).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarBodyIlegible(
            HttpMessageNotReadableException exception) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no se pudo interpretar. "
                        + "Revisá el formato del JSON y los tipos de los campos.");
    }

    /**
     * Un query param con un tipo que no se puede convertir, por ejemplo
     * {@code ?limite=abc} o un UUID mal formado.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manageTipoInvalido(
            MethodArgumentTypeMismatchException exception) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El valor '" + exception.getValue() + "' no es válido para el campo '"
                        + exception.getName() + "'");
    }

    /**
     * Se lanza cuando una entidad referenciada no existe. Antes caía en el Manejo
     * genérico de Spring y devolvía 500; corresponde a un 404.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> manejarEntidadNoEncontrada(
            EntityNotFoundException exception) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * Violación de integridad referencial: por ejemplo, borrar una categoría que
     * todavía tiene donantes asignados. Es un conflicto con el estado actual de los
     * datos, no un error del servidor.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarIntegridad(
            DataIntegrityViolationException exception) {
        return respuesta(HttpStatus.CONFLICT,
                "La operación no se puede completar porque hay datos relacionados "
                        + "que la impiden. Revisá si el recurso está en uso.");
    }

    private ResponseEntity<Map<String, String>> respuesta(
            HttpStatus status,
            String mensaje
    ) {
        return ResponseEntity.status(status)
                .body(Map.of("error", mensaje == null ? status.getReasonPhrase() : mensaje));
    }
}
