package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.mappers.NotificacionMapper;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Servicio de Notificaciones", description = "Endpoints para la recepción, encolamiento y despacho de alertas del sistema (Mails, Mensajería, etc.).")
public class NotificadorController {

    private static final Logger log = LoggerFactory.getLogger(NotificadorController.class);

    private final NotificadorService notificadorService;
    private final NotificacionMapper notificacionMapper;

    public NotificadorController(NotificadorService notificadorService, NotificacionMapper notificacionMapper) {
        this.notificadorService = notificadorService;
        this.notificacionMapper = notificacionMapper;
    }

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
    public ResponseEntity<String> recibirSolicitudNotificacion(
            @Valid @RequestBody SolicitudNotificacionDTO dto) {
        log.info("[HTTP] POST /api/notificaciones | medio='{}', destino='{}', asunto='{}'",
                dto.getMedioDeContacto(), dto.getDireccionDeContacto(), dto.getAsuntoMensaje());

        notificadorService.procesarSolicitudDeNotificacion(dto);

        log.info("[HTTP] Solicitud aceptada (202) para '{}'", dto.getDireccionDeContacto());
        return new ResponseEntity<>("solicitud procesada con éxito", HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Ver notificacion por id")
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionDTO> obtenerNotificacion(@PathVariable UUID id) {
        Optional<Notificacion> notificacion = notificadorService.obtenerPorId(id);
        return notificacion
                .map(d -> ResponseEntity.ok(notificacionMapper.notificacionDTO(d)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
