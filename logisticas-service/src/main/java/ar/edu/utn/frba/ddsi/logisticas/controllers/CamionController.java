package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionesDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.CamionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
  @Operation(summary = "Listar camiones de forma paginada")
  @GetMapping
  public ResponseEntity<Page<CamionDTO>> obtenerTodos(
          @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(camionService.findAll(pageable));
  }

  @Operation(summary = "Obtener un camión por su patente")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Camion encontrado"),
          @ApiResponse(responseCode = "404", description = "Camión no encontrado")
  })
  @GetMapping("/{patente}")
  public ResponseEntity<?> obtenerPorPatente(@PathVariable String patente) {
    return ResponseEntity.ok(camionService.findById(patente));
  }

  @Operation(summary = "Registrar un nuevo camión")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "201", description = "Camión registrado exitosamente"),
          @ApiResponse(responseCode = "400", description = "Datos del camión inválidos"),
          @ApiResponse(responseCode = "409", description = "La patente ya se encuentra registrada")
  })
  @PostMapping
  public ResponseEntity<?> crearCamion(@Valid @RequestBody CamionDTO request) {
    CamionDTO nuevoCamion = camionService.create(request);
    return new ResponseEntity<>(nuevoCamion, HttpStatus.CREATED);
  }

  @Operation(summary = "Registrar múltiples camiones")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "201", description = "Camiones registrados exitosamente"),
          @ApiResponse(responseCode = "400", description = "Datos de los camiones inválidos"),
          @ApiResponse(responseCode = "409", description = "Conflicto por patente duplicada")
  })
  @PostMapping("/registrar")
  public ResponseEntity<?> registrarCamiones(@Valid @RequestBody CamionesDTO camiones) {
    CamionesDTO nuevosCamiones = camionService.createMultiple(camiones);
    return new ResponseEntity<>(nuevosCamiones, HttpStatus.CREATED);
  }

  @Operation(summary = "Actualizar un camión existente")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Camion actualizado"),
          @ApiResponse(responseCode = "404", description = "Camión no encontrado")
  })
  @PutMapping("/{patente}")
  public ResponseEntity<?> actualizarCamion(@PathVariable String patente, @RequestBody CamionDTO request) {
    return ResponseEntity.ok(camionService.update(patente, request));
  }

  @Operation(summary = "Eliminar un camión")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "Camion eliminado"),
          @ApiResponse(responseCode = "404", description = "Camión no encontrado")
  })
  @DeleteMapping("/{patente}")
  public ResponseEntity<?> eliminarCamion(@PathVariable String patente) {
    camionService.delete(patente);
    return ResponseEntity.noContent().build();
  }

  // --- OPERACIONES DE NEGOCIO ---

  @Operation(summary = "Actualizar disponibilidad de un camión")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
      @ApiResponse(responseCode = "400", description = "Falta el campo 'disponible'"),
      @ApiResponse(responseCode = "404", description = "Camión no encontrado")
  })
  @PatchMapping("/{patente}/estado")
  public ResponseEntity<String> cambiarEstadoDisponibilidad(
      @PathVariable String patente,
      @RequestBody Map<String, Boolean> body) {
    return ResponseEntity.ok(camionService.cambiarDisponibilidad(patente, body));
  }
}
