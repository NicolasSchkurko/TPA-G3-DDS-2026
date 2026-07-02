package ar.edu.utn.frba.ddsi.logisticas.controllers;


import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.services.EventoLogisticaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logistica/eventos")
public class EventoLogisticaController {

  private final EventoLogisticaService eventoService;

  public EventoLogisticaController(EventoLogisticaService eventoService) {
    this.eventoService = eventoService;
  }

  /**
   * ENDPOINT DE HTTP POLLING.
   * Ejemplo de uso: GET /api/logistica/eventos?desdeId=15
   */
  @GetMapping
  public ResponseEntity<List<EventoLogistica>> obtenerEventos(
      @RequestParam(defaultValue = "0") Long desdeId) {

    List<EventoLogistica> eventos = eventoService.obtenerEventosNuevos(desdeId);
    return ResponseEntity.ok(eventos);
  }
}