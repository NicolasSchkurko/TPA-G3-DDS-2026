package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionesDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.CamionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DuplicateKeyException;
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
  @Operation(summary = "Listar todos los camiones")
  @GetMapping
  public ResponseEntity<CamionesDTO> obtenerTodos() {
    return ResponseEntity.ok(camionService.findAll());
  }

  @Operation(summary = "Obtener un camión por su patente")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Camion encontrado"),
          @ApiResponse(responseCode = "404", description = "Camión no encontrado")
  })
  @GetMapping("/{patente}")
  public ResponseEntity<?> obtenerPorPatente(@PathVariable String patente) {
    try {
      return ResponseEntity.ok(camionService.findById(patente));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  @Operation(summary = "Registrar un nuevo camión")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "201", description = "Camión registrado exitosamente"),
          @ApiResponse(responseCode = "400", description = "Datos del camión inválidos"),
          @ApiResponse(responseCode = "409", description = "La patente ya se encuentra registrada")
  })
  @PostMapping
  public ResponseEntity<?> crearCamion(@RequestBody CamionDTO request) {
    try {
      CamionDTO nuevoCamion = camionService.create(request);
      return new ResponseEntity<>(nuevoCamion, HttpStatus.CREATED);
    } catch (DuplicateKeyException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
              .body("Ya existe un camión registrado con la patente: " + (request != null ? request.getPatente() : ""));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error al registrar el camión: " + e.getMessage());
    }
  }

  @Operation(summary = "Registrar múltiples camiones")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "201", description = "Camiones registrados exitosamente"),
          @ApiResponse(responseCode = "400", description = "Datos de los camiones inválidos"),
          @ApiResponse(responseCode = "409", description = "Conflicto por patente duplicada")
  })
  @PostMapping("/registrar")
  public ResponseEntity<?> registrarCamiones(@RequestBody CamionesDTO camiones) {
    try {
      CamionesDTO nuevosCamiones = camionService.createMultiple(camiones);
      return new ResponseEntity<>(nuevosCamiones, HttpStatus.CREATED);
    } catch (DuplicateKeyException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
              .body("Error de clave duplicada: una o más patentes ya se encuentran registradas");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error al registrar camiones: " + e.getMessage());
    }
  }

  @Operation(summary = "Actualizar un camión existente")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Camion actualizado"),
          @ApiResponse(responseCode = "404", description = "Camión no encontrado")
  })
  @PutMapping("/{patente}")
  public ResponseEntity<?> actualizarCamion(@PathVariable String patente, @RequestBody CamionDTO request) {
    try {
      return ResponseEntity.ok(camionService.update(patente, request));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  @Operation(summary = "Eliminar un camión")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "Camion eliminado"),
          @ApiResponse(responseCode = "404", description = "Camión no encontrado")
  })
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
      String mensaje = camionService.cambiarDisponibilidad(patente, body);
      return ResponseEntity.ok(mensaje);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }
}
