package ar.edu.utn.frba.ddsi.notificaciones.controllers;



import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notificaciones")
public class NotificadorController {
    private final NotificadorService notificadorService;
    public NotificadorController(NotificadorService notificadorService) {
        this.notificadorService = notificadorService;
    }

    // Ruta para crear: POST /notificaciones
    @PostMapping
    public ResponseEntity<String> recibirSolicitudNotificacion(@RequestBody SolicitudNotificacionDTO dto) {
        try {
            notificadorService.procesarSolicitudDeNotificacion(dto);
            return new ResponseEntity<>("solicitud procesada con éxito", HttpStatus.ACCEPTED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}