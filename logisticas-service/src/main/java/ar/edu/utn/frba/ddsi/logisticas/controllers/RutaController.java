package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.RutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.services.RutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rutas")
@Tag(name = "Rutas", description = "API para el control CRUD y el ciclo de vida de las Rutas de reparto")
public class RutaController {

  private final RutaService rutaService;

  public RutaController(RutaService rutaService) {
    this.rutaService = rutaService;
  }

  // --- CRUD ---

  @Operation(summary = "Listar todas las rutas generadas")
  @GetMapping
  public ResponseEntity<List<Ruta>> obtenerTodas() {
    return ResponseEntity.ok(rutaService.findAll());
  }

  @Operation(summary = "Obtener una ruta por su ID")
  @GetMapping("/{id}")
  public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(rutaService.findById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  @Operation(summary = "Crear una ruta manual", description = "Usualmente generadas por el servicio planificador.")
  @PostMapping
  public ResponseEntity<Ruta> crearRuta(@RequestBody Ruta request) {
    return new ResponseEntity<>(rutaService.create(request), HttpStatus.CREATED);
  }

  @Operation(summary = "Actualizar datos de una ruta")
  @PutMapping("/{id}")
  public ResponseEntity<?> actualizarRuta(@PathVariable UUID id, @RequestBody Ruta ruta) {
    try {
      return ResponseEntity.ok(rutaService.update(id, ruta));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  @Operation(summary = "Eliminar una ruta")
  @DeleteMapping("/{id}")
  public ResponseEntity<?> eliminarRuta(@PathVariable UUID id) {
    try {
      rutaService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  // --- OPERACIONES DE NEGOCIO ---

  @Operation(summary = "El chofer informa el inicio de su ruta",
      description = "Cambia el estado de la ruta a EN_CURSO y transiciona los ítems a EN_TRASLADO. Notifica vía eventos.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ruta iniciada con éxito."),
      @ApiResponse(responseCode = "400", description = "Validación de negocio fallida.")
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

  @Operation(summary = "El chofer finaliza su jornada y ruta activa",
      description = "Finaliza la ruta, limpia el camión y retorna los ítems no entregados a PENDIENTE.")
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