package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import ar.edu.utn.frba.ddsi.notificaciones.controllers.request.NotificacionFiltroRequest;
import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.SolicitudAceptadaDTO;
import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.mappers.NotificacionMapper;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Servicio de Notificaciones", description = "Endpoints para la recepción, encolamiento y despacho de alertas del sistema (Mails, Mensajería, etc.).")
public class NotificadorController {

    private static final Logger log = LoggerFactory.getLogger(NotificadorController.class);

    private final NotificadorService notificadorService;
    private final NotificacionMapper notificacionMapper;
    private final MedioDeEnvioFactory medioDeEnvioFactory;

    public NotificadorController(NotificadorService notificadorService,
                                 NotificacionMapper notificacionMapper,
                                 MedioDeEnvioFactory medioDeEnvioFactory) {
        this.notificadorService = notificadorService;
        this.notificacionMapper = notificacionMapper;
        this.medioDeEnvioFactory = medioDeEnvioFactory;
    }

    @Operation(
            summary = "Recibir y procesar solicitud de notificación",
            description = "Punto de entrada asincrónico para que otros microservicios soliciten el envío de un mensaje. " +
                    "El sistema valida la estructura y acepta la petición para ser procesada en segundo plano por el motor de notificaciones."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Solicitud aceptada y encolada; devuelve el id y la cabecera Location del recurso"),
            @ApiResponse(responseCode = "400", description = "Error de validación en los datos de la solicitud (cuerpo malformado, faltan campos obligatorios o medio de contacto desconocido)")
    })
    @PostMapping
    public ResponseEntity<SolicitudAceptadaDTO> recibirSolicitudNotificacion(
            @Valid @RequestBody SolicitudNotificacionDTO dto) {
        log.info("[HTTP] POST /api/notificaciones | medio='{}', destino='{}', asunto='{}'",
                dto.getMedioDeContacto(), dto.getDireccionDeContacto(), dto.getAsuntoMensaje());

        // Validación en el borde: un medio fuera del contrato se rechaza con 400 antes de persistir.
        // La excepción la traduce GlobalExceptionHandler a 400 Bad Request.
        medioDeEnvioFactory.mapearAMedioEnvio(dto.getMedioDeContacto());

        UUID id = notificadorService.procesarSolicitudDeNotificacion(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQuery(null)
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();

        log.info("[HTTP] Solicitud aceptada (202) para '{}' con id {}", dto.getDireccionDeContacto(), id);
        return ResponseEntity.accepted()
                .location(location)
                .body(new SolicitudAceptadaDTO(id.toString()));
    }

    @Operation(summary = "Reintentar el envío de una notificación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Reintento encolado; se vuelve a publicar el aviso"),
            @ApiResponse(responseCode = "404", description = "La notificación no existe"),
            @ApiResponse(responseCode = "409", description = "La notificación ya fue enviada o está en curso")
    })
    @PostMapping("/{id}/reintento")
    public ResponseEntity<SolicitudAceptadaDTO> reintentarNotificacion(@PathVariable UUID id) {
        notificadorService.reintentar(id);

        log.info("[HTTP] Reintento aceptado (202) para la notificación {}", id);
        return ResponseEntity.accepted().body(new SolicitudAceptadaDTO(id.toString()));
    }

    @Operation(summary = "Listar notificaciones paginadas, con filtro opcional por estado")
    @GetMapping(params = "!destinatario")
    public ResponseEntity<Page<NotificacionDTO>> listarNotificaciones(
            @ParameterObject @ModelAttribute NotificacionFiltroRequest filtros,
            @PageableDefault(page = 0, size = 10, sort = "fechaCreacion",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificadorService.listar(filtros.estado(), pageable));
    }

    @Operation(summary = "Ver notificacion por id")
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionDTO> obtenerNotificacion(@PathVariable UUID id) {
        Optional<Notificacion> notificacion = notificadorService.obtenerPorId(id);
        return notificacion
                .map(d -> ResponseEntity.ok(notificacionMapper.notificacionDTO(d)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Listar notificaciones por destinatario",
            description = "Devuelve las notificaciones dirigidas a un contacto (email o teléfono), " +
                    "más recientes primero. Pensado para que el resto de los servicios (o el front) " +
                    "arme la bandeja de avisos del usuario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de notificaciones (puede estar vacío)"),
            @ApiResponse(responseCode = "400", description = "Falta el destinatario o el estado no es válido")
    })
    @GetMapping(params = "destinatario")
    public ResponseEntity<List<NotificacionDTO>> listarNotificaciones(
            @RequestParam String destinatario,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("[HTTP] GET /api/notificaciones | destinatario='{}', estado='{}', page={}, size={}",
                destinatario, estado, page, size);
        List<NotificacionDTO> notificaciones = notificadorService.listarPorDestinatario(destinatario, estado, page, size)
                .stream()
                .map(notificacionMapper::notificacionDTO)
                .toList();
        return ResponseEntity.ok(notificaciones);
    }
}
