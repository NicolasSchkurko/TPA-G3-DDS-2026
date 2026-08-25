package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/notificaciones")
@Tag(name = "Servicio de Notificaciones", description = "Endpoints para la recepción, encolamiento y despacho de alertas del sistema (Mails, Mensajería, etc.).")
public class NotificadorController {
    private final NotificadorService notificadorService;
    private final NotificacionMapper NotificacionMapper;

    public NotificadorController(NotificadorService notificadorService, NotificacionMapper notificacionMapper) {
        this.notificadorService = notificadorService;
        NotificacionMapper = notificacionMapper;
    }

    // Ruta para crear: POST /notificaciones
    @Operation(
            summary = "Recibir y procesar solicitud de notificación",
            description = "Punto de entrada asincrónico para que otros microservicios soliciten el envío de un mensaje. " +
                    "El sistema valida la estructura y acepta la petición para ser procesada en segundo plano por el motor de notificaciones."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Solicitud aceptada y encolada con éxito para su procesamiento"),
            @ApiResponse(responseCode = "400", description = "Error de validación en los datos de la solicitud (cuerpo malformado o faltan campos obligatorios)")
    })
    @PostMapping
    public ResponseEntity<String> recibirSolicitudNotificacion(@RequestBody SolicitudNotificacionDTO dto) {
        try {
            notificadorService.procesarSolicitudDeNotificacion(dto);
            return new ResponseEntity<>("solicitud procesada con éxito", HttpStatus.ACCEPTED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Ver notificacion por id")
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionDTO> obtenerNotificacion(@PathVariable UUID id) {
        Optional<Notificacion> notificacion = NotificadorService.obtenerPorId(id);
        return notificacion.map(d -> ResponseEntity.ok(NotificacionMapper.notificacionDTO(d)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}