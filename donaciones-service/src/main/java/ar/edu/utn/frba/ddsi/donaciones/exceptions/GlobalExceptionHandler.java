package ar.edu.utn.frba.ddsi.donaciones.exceptions;

import ar.edu.utn.frba.ddsi.donaciones.dto.errors.ErrorResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // Captura los errores de validación que lanzaste en tu Service. Antes no logueaba nada: un
  // 400 (p. ej. "donante no encontrado" en POST /donaciones/formulario) no dejaba ningún rastro
  // en el log del servidor, solo el mensaje en el body de la respuesta.
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponseDTO> manejarIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("400 por IllegalArgumentException: {}", ex.getMessage(), ex);
    ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  // Fallos de Bean Validation (@Valid) en el body de la request: sin este handler caían en el
  // catch-all de Exception y se devolvían como 500 en vez de 400.
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> manejarMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
    String mensaje = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
    log.warn("400 por validación de body ({}): {}", ex.getParameter().getExecutable().getName(), mensaje);
    ErrorResponseDTO error = new ErrorResponseDTO(mensaje, HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  // El único "no encontrado" real: antes los controllers atajaban cualquier RuntimeException
  // y la devolvían como 404, lo que escondía NPEs y violaciones de FK bajo un 404 limpio.
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> manejarEntityNotFoundException(EntityNotFoundException ex) {
    log.debug("404: {}", ex.getMessage());
    ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), HttpStatus.NOT_FOUND.value());
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDTO> manejarDataIntegrityViolationException(DataIntegrityViolationException ex) {
    log.warn("409 por violación de integridad de datos: {}", ex.getMessage(), ex);
    ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), HttpStatus.CONFLICT.value());
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  // "sumidero" (catch-all) para cualquier excepción no reconocida, devuelve un 500 genérico.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> manejarErroresInesperados(Exception ex) {
    log.error("500 por excepción inesperada", ex);

    ErrorResponseDTO error = new ErrorResponseDTO(
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value());

    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}