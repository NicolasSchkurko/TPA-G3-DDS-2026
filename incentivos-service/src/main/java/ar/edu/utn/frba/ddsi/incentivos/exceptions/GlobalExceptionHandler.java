package ar.edu.utn.frba.ddsi.incentivos.exceptions;

import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones de la capa de servicios a códigos HTTP con un mensaje.
 *
 * <p>Sin esto, casi todo terminaba en 500: un 404, un 409 o un 400 llegaban al cliente
 * como "error interno del servidor", que no dice nada de qué corregir.
 *
 * <p>El mapeo es:
 *
 * <ul>
 *   <li>403: {@code SecurityException}, o sea el id del header no es de un admin.
 *   <li>404: {@code InexistenteException} y {@code EntityNotFoundException}.
 *   <li>409: {@code ConflictoException}, {@code PerfilExistenteException} y
 *       {@code DataIntegrityViolationException}, que es lo que tira la base cuando el
 *       borrado dejaría referencias colgando.
 *   <li>400: datos inválidos del cuerpo o de la query.
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** El header {@code Admin-Id} no corresponde a un administrador: 403. */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> manejarAccesoDenegado(
            SecurityException exception) {
        return respuesta(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    /** El recurso pedido no existe: 404. */
    @ExceptionHandler(InexistenteException.class)
    public ResponseEntity<Map<String, String>> manejarInexistente(
            InexistenteException exception) {
        String mensaje = exception.getMessage() == null
                ? "El recurso solicitado no existe"
                : exception.getMessage();
        return respuesta(HttpStatus.NOT_FOUND, mensaje);
    }

    /** El usuario ya tiene un perfil: 409, y el mensaje dice con qué id. */
    @ExceptionHandler(PerfilExistenteException.class)
    public ResponseEntity<Map<String, String>> manejarPerfilExistente(
            PerfilExistenteException exception) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage());
    }

    /**
     * La operación es válida en forma pero no con estos datos, por ejemplo la posición en
     * la secuencia ya ocupada: 409.
     */
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, String>> manejarConflicto(
            ConflictoException exception) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage());
    }

    /** El servicio rechazó los datos antes de tocar la base: 400, con el motivo en el mensaje. */
    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<Map<String, String>> manejarDatosInvalidos(
            DatosInvalidosException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /** Se lanzó un {@code IllegalArgumentException} en la capa de servicios: 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(
            IllegalArgumentException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /** Falta un header obligatorio, típicamente el {@code Admin-Id}: 400, diciendo cuál. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, String>> manejarHeaderFaltante(
            MissingRequestHeaderException exception) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "Falta el header requerido: " + exception.getHeaderName());
    }

    /**
     * Falta la categoría base "Colaborador", que es un problema de los datos y no del
     * pedido, así que responde 500.
     */
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
    public ResponseEntity<Map<String, String>> manejarTipoInvalido(
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
