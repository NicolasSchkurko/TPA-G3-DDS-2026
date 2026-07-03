package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.CamionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camiones")
@Tag(name = "Camiones", description = "API CRUD y operaciones de negocio para la gestión de Camiones")
public class CamionController {

  private final CamionService camionService;

  public CamionController(CamionService camionService) {
    this.camionService = camionService;
  }

  // --- CRUD ---

  @Operation(summary = "Listar todos los camiones")
  @GetMapping
  public ResponseEntity<List<CamionDTO>> obtenerTodos() {
    return ResponseEntity.ok(camionService.findAll());
  }

  @Operation(summary = "Obtener un camión por su patente")
  @GetMapping("/{patente}")
  public ResponseEntity<?> obtenerPorPatente(@PathVariable String patente) {
    try {
      return ResponseEntity.ok(camionService.findById(patente));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  @Operation(summary = "Registrar un nuevo camión")
  @PostMapping
  public ResponseEntity<CamionDTO> crearCamion(@RequestBody CamionDTO request) {
    CamionDTO nuevoCamion = camionService.create(request);
    return new ResponseEntity<>(nuevoCamion, HttpStatus.CREATED);
  }

  @Operation(summary = "Actualizar un camión existente")
  @PutMapping("/{patente}")
  public ResponseEntity<?> actualizarCamion(@PathVariable String patente, @RequestBody CamionDTO request) {
    try {
      return ResponseEntity.ok(camionService.update(patente, request));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  @Operation(summary = "Eliminar un camión")
  @DeleteMapping("/{patente}")
  public ResponseEntity<?> eliminarCamion(@PathVariable String patente) {
    try {
      camionService.delete(patente);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  // --- OPERACIONES DE NEGOCIO ---

  @Operation(summary = "Actualizar disponibilidad de un camión")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
      @ApiResponse(responseCode = "404", description = "Camión no encontrado")
  })
  @PatchMapping("/{patente}/estado")
  public ResponseEntity<String> cambiarEstadoDisponibilidad(
      @PathVariable String patente,
      @RequestBody Map<String, Boolean> body) {
    try {
      Boolean disponible = body.get("disponible");
      if (disponible != null && disponible) {
        camionService.marcarDisponible(patente);
        return ResponseEntity.ok("Camión marcado como disponible.");
      } else {
        camionService.marcarOcupado(patente);
        return ResponseEntity.ok("Camión marcado como ocupado.");
      }
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }
}