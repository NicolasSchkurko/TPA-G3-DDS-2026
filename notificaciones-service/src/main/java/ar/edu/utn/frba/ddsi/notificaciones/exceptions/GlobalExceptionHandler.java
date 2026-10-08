package ar.edu.utn.frba.ddsi.notificaciones.exceptions;

import ar.edu.utn.frba.ddsi.notificaciones.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Traduce excepciones a un cuerpo de error uniforme. Extiende {@link ResponseEntityExceptionHandler}
 * para no pisar los status propios de Spring MVC (404, 405, 415, binding).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Errores de validación del body: 400 con el detalle de cada campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describir)
                .toList();

        log.warn("Solicitud rechazada por validación: {}", detalles);

        return ResponseEntity.badRequest().body(new ErrorResponseDTO(
                "La solicitud tiene campos obligatorios faltantes o inválidos",
                HttpStatus.BAD_REQUEST.value(),
                detalles));
    }

    private String describir(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    /** Una violación de integridad es un dato mal formado que se coló: 400, con el SQL solo en el log. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> manejarIntegridad(DataIntegrityViolationException ex) {
        log.warn("La solicitud violó una restricción de la base de datos", ex);

        return ResponseEntity.badRequest().body(new ErrorResponseDTO(
                "La solicitud no cumple una restricción de datos",
                HttpStatus.BAD_REQUEST.value()));
    }

    /** Datos inválidos detectados por el dominio (por ejemplo, un medio de contacto desconocido). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> manejarIllegalArgument(IllegalArgumentException ex) {
        log.warn("Solicitud inválida: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(new ErrorResponseDTO(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()));
    }

    /** Respeta el status elegido con {@code throw new ResponseStatusException(...)}. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> manejarResponseStatus(ResponseStatusException ex) {
        String mensaje = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();

        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponseDTO(
                mensaje,
                ex.getStatusCode().value()));
    }

    /** Red de contención: 500 genérico, con el error real en el log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarInesperado(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);

        return ResponseEntity.internalServerError().body(new ErrorResponseDTO(
                "Ocurrió un error interno en el servidor",
                HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
