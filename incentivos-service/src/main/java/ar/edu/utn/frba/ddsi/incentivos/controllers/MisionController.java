package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.MisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/misiones")
@Tag(name = "Administración - Misiones", description = "Gestión de misiones del sistema gamificado.")
public class MisionController {

  private final MisionService service;

  public MisionController(MisionService service) {
    this.service = service;
  }

  // TODO: PAGINACION
  @Operation(
      summary = "Obtener todas las misiones",
      description = "Retorna la lista completa de misiones disponibles en el sistema."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Misiones obtenidas con éxito"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @GetMapping
  public ResponseEntity<List<MisionDTO>> obtenerMisiones() {
    return ResponseEntity.ok(service.obtenerMisiones());
  }

  @Operation(
      summary = "Obtener una misión específica",
      description = "Retorna los detalles de una misión identificada por su UUID."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Misión obtenida con éxito"),
      @ApiResponse(responseCode = "404", description = "Misión no encontrada"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @GetMapping("/{id}")
  public ResponseEntity<MisionDTO> obtenerMisionPorId(
      @Parameter(description = "UUID de la misión", required = true)
      @PathVariable UUID id
  ) {
    MisionDTO mision = service.obtenerMisionPorId(id);
    return mision == null
           ? ResponseEntity.notFound().build()
           : ResponseEntity.ok(mision);
  }

  @Operation(
      summary = "Crear una nueva misión",
      description = "Crea una nueva misión en el sistema."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Misión creada con éxito"),
      @ApiResponse(responseCode = "400", description = "Datos inválidos"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @PostMapping("/admin")
  public ResponseEntity<MisionDTO> crearMision(
      @Parameter(description = "UUID del administrador", required = true)
      @RequestHeader("Admin-Id") UUID idAdmin,
      @RequestBody MisionDTO request
  ) {
    MisionDTO nuevaMision = service.crearMision(idAdmin, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(nuevaMision);
  }

  @Operation(
      summary = "Actualizar una misión",
      description = "Actualiza los datos de una misión existente."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Misión actualizada con éxito"),
      @ApiResponse(responseCode = "404", description = "Misión no encontrada"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @PutMapping("/admin/{id}")
  public ResponseEntity<MisionDTO> actualizarMision(
      @Parameter(description = "UUID del administrador", required = true)
      @RequestHeader("Admin-Id") UUID idAdmin,
      @Parameter(description = "UUID de la misión a actualizar", required = true)
      @PathVariable UUID id,
      @RequestBody MisionDTO mision
  ) {
    MisionDTO actualizada = service.actualizarMision(idAdmin, id, mision);
    return actualizada == null
           ? ResponseEntity.notFound().build()
           : ResponseEntity.ok(actualizada);
  }

  @Operation(
      summary = "Eliminar una misión",
      description = "Elimina una misión del sistema."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Misión eliminada con éxito"),
      @ApiResponse(responseCode = "404", description = "Misión no encontrada"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @DeleteMapping("/admin/{id}")
  public ResponseEntity<Void> eliminarMision(
      @Parameter(description = "UUID del administrador", required = true)
      @RequestHeader("Admin-Id") UUID idAdmin,
      @Parameter(description = "UUID de la misión a eliminar", required = true)
      @PathVariable UUID id
  ) {
    service.eliminarMision(idAdmin, id);
    return ResponseEntity.noContent().build();
  }
}