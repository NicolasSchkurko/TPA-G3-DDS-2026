package ar.edu.utn.frba.ddsi.notificaciones.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/*
@RestControllerAdvice
public class GlobalExceptionHandler {

  // Captura los errores de validación que lanzaste en tu Service
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponseDTO> manejarIllegalArgumentException(IllegalArgumentException ex) {
    ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  // m"sumidero" (catch‑all) para cualquier excepción no reconocida, devuelve un 500 genérico.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> manejarErroresInesperados(Exception ex) {
    ErrorResponseDTO error = new ErrorResponseDTO("Ocurrió un error interno en el servidor", HttpStatus.INTERNAL_SERVER_ERROR.value());

    // Aquí también podrías hacer un log del error real (ex.printStackTrace())
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}*/