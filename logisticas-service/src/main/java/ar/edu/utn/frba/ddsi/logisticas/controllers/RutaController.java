package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.rutas.RutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.RutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

  @Operation(summary = "Listar de forma paginada las rutas generadas")
  @GetMapping
  public ResponseEntity<Page<RutaDTO>> obtenerTodas(
          @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(rutaService.findAll(pageable));
  }

  @Operation(summary = "Obtener una ruta por su ID")
  @GetMapping("/{id}")
  public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
    return ResponseEntity.ok(rutaService.findById(id));
  }

  @Operation(summary = "Listar las rutas activas (programadas o en curso)")
  @GetMapping("/activas")
  public ResponseEntity<List<RutaDTO>> obtenerActivas() {
    return ResponseEntity.ok(rutaService.findActivas());
  }

  @Operation(summary = "Listar el historial paginado de rutas de un chofer")
  @GetMapping("/chofer/{idChofer}")
  public ResponseEntity<Page<RutaDTO>> obtenerPorChofer(
          @PathVariable UUID idChofer,
          @ParameterObject @PageableDefault(size = 8, sort = "fechaProgramada", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(rutaService.findByChofer(idChofer, pageable));
  }

  @Operation(summary = "Obtener la ruta activa de un chofer (en curso o programada)")
  @GetMapping("/chofer/{idChofer}/activa")
  public ResponseEntity<RutaDTO> obtenerActiva(@PathVariable UUID idChofer) {
    return rutaService.findActiva(idChofer)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @Operation(summary = "El chofer informa el inicio de su ruta",
      description = "Cambia el estado de la ruta a EN_CURSO y transiciona los ítems a EN_TRASLADO. Notifica vía eventos.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ruta iniciada con éxito."),
      @ApiResponse(responseCode = "400", description = "Payload inválido."),
      @ApiResponse(responseCode = "404", description = "Chofer o ruta no encontrados."),
      @ApiResponse(responseCode = "409", description = "El chofer no tiene una ruta PROGRAMADA.")
  })
  @PatchMapping("/chofer/{idChofer}/iniciar")
  public ResponseEntity<String> iniciarRuta(@PathVariable UUID idChofer) {
    rutaService.iniciarRuta(idChofer);
    return ResponseEntity.ok("Ruta iniciada correctamente. Los paquetes asociados se encuentran en traslado.");
  }

  @Operation(summary = "El chofer finaliza su jornada y ruta activa",
      description = "Finaliza la ruta, limpia el camión y retorna los ítems no entregados a PENDIENTE.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ruta finalizada con éxito."),
      @ApiResponse(responseCode = "404", description = "Chofer o ruta no encontrados."),
      @ApiResponse(responseCode = "409", description = "El chofer no tiene una ruta EN_CURSO.")
  })
  @PatchMapping("/chofer/{idChofer}/terminar")
  public ResponseEntity<String> terminarRuta(@PathVariable UUID idChofer) {
    rutaService.terminarRuta(idChofer);
    return ResponseEntity.ok("Ruta finalizada correctamente. Capacidades de carga del camión restablecidas.");
  }
}