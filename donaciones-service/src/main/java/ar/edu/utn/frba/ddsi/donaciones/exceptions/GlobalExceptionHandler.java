package ar.edu.utn.frba.ddsi.donaciones.exceptions;

import ar.edu.utn.frba.ddsi.donaciones.dto.errors.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Los errores de Bean Validation (@Valid en los @RequestBody, punto 22) son un 400 con
   * detalle: sin este handler caerían en el catch-all de abajo como un 500 con el
   * mensaje de la excepción, que no dice qué campo falló.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> manejarValidacion(MethodArgumentNotValidException ex) {
    String detalle = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

    log.warn("Request rechazado por validación: {}", detalle);

    ErrorResponseDTO error = new ErrorResponseDTO(detalle, HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  // Captura los errores de validación que lanzaste en tu Service
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponseDTO> manejarIllegalArgumentException(IllegalArgumentException ex) {
    ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  // m"sumidero" (catch‑all) para cualquier excepción no reconocida, devuelve un 500 genérico.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> manejarErroresInesperados(Exception ex) {

    System.err.println("ERROR CAPTURADO:");
    ex.printStackTrace();

    ErrorResponseDTO error = new ErrorResponseDTO(
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value());

    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}