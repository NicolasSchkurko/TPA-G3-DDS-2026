package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.services.RutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rutas")
@Tag(name = "Rutas", description = "API para el control operacional de las rutas y su ciclo de vida")
public class RutaController {

  private final RutaService rutaService;

  public RutaController(RutaService rutaService) {
    this.rutaService = rutaService;
  }

  @Operation(summary = "Iniciar la ruta de un chofer",
      description = "Cambia el estado de la ruta activa a EN_CURSO y setea las entregas en estado EN_TRASLADO.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ruta iniciada con éxito. Evento publicado."),
      @ApiResponse(responseCode = "400", description = "El chofer especificado no existe o no posee rutas asignadas")
  })
  @PatchMapping("/chofer/{idChofer}/iniciar")
  public ResponseEntity<String> iniciarRuta(@PathVariable UUID idChofer) {
    try {
      rutaService.iniciarRuta(idChofer);
      return ResponseEntity.ok("Ruta iniciada correctamente. Los paquetes asociados se encuentran en traslado.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Error interno en el servidor: " + e.getMessage());
    }
  }

  @Operation(summary = "Finalizar la ruta de un chofer",
      description = "Concluye la ruta del día. Los paquetes no entregados retornan a estado PENDIENTE y se limpia el camión.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ruta finalizada y recursos liberados con éxito"),
      @ApiResponse(responseCode = "400", description = "Error al procesar la finalización")
  })
  @PatchMapping("/chofer/{idChofer}/terminar")
  public ResponseEntity<String> terminarRuta(@PathVariable UUID idChofer) {
    try {
      rutaService.terminarRuta(idChofer);
      return ResponseEntity.ok("Ruta finalizada correctamente. Capacidades de carga del camión restablecidas.");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Error interno al finalizar la ruta: " + e.getMessage());
    }
  }
}