package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.services.EventoLogisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logistica/eventos")
@Tag(name = "Eventos de Logística (Polling)", description = "API para la consulta de eventos de trazabilidad generados por el módulo logístico mediante HTTP Polling")
public class EventoLogisticaController {

  private final EventoLogisticaService eventoService;

  public EventoLogisticaController(EventoLogisticaService eventoService) {
    this.eventoService = eventoService;
  }

  /**
   * ENDPOINT DE HTTP POLLING.
   * Ejemplo de uso: GET /api/logistica/eventos?desdeId=15
   */
  @Operation(summary = "Obtener eventos logísticos nuevos",
      description = "Permite a otros módulos (como el Servicio de Notificaciones) consultar los últimos eventos logísticos (inicio de rutas, entregas, fallos) utilizando un ID de referencia (desdeId).")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lista de eventos recuperada con éxito")
  })
  @GetMapping
  public ResponseEntity<List<EventoLogistica>> obtenerEventos(
      @Parameter(description = "ID del último evento que el cliente ya procesó. Se devolverán los eventos con ID estrictamente mayor a este valor.", example = "15")
      @RequestParam(defaultValue = "0") Long desdeId) {

    List<EventoLogistica> eventos = eventoService.obtenerEventosNuevos(desdeId);
    return ResponseEntity.ok(eventos);
  }
}