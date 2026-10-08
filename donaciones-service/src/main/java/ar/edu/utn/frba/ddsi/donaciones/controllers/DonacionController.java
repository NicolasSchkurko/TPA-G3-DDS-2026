package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.AsignarPropuestaRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.CambioEstadoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/donaciones")
@Tag(name = "Servicio de donaciones", description = "Endpoints para operaciones CRUD de Donaciones")
public class DonacionController {

  private final DonacionService donacionService;
  private final RabbitTemplate rabbitTemplate;

  public DonacionController(DonacionService donacionService, RabbitTemplate rabbitTemplate) {
    this.donacionService = donacionService;
    this.rabbitTemplate = rabbitTemplate;
  }

  @Operation(summary = "Crear una Donación")
  @PostMapping("/formulario")
  public ResponseEntity<List<DonacionDTO>> crearDonacion(@Valid @RequestBody FormularioRequestDTO request) {
    // procesarFormulario ya no devuelve null: tira IllegalArgumentException (-> 400 vía
    // GlobalExceptionHandler) si no encuentra al donante.
    return ResponseEntity.ok(donacionService.procesarFormulario(request));
  }

  @Operation(summary = "Ver donaciones")
  @GetMapping
  public ResponseEntity<List<DonacionDTO>> obtenerDonaciones() {
    return ResponseEntity.ok(donacionService.obtenerTodas());
  }

  @Operation(summary = "Ver donación por id")
  @GetMapping("/{id}")
  public ResponseEntity<DonacionDTO> obtenerDonacion(@PathVariable UUID id) {
    // Sin catch: GlobalExceptionHandler mapea EntityNotFoundException -> 404 por tipo,
    // en vez de que cualquier RuntimeException (NPE incluido) se lea como "no existe".
    return ResponseEntity.ok(donacionService.obtenerPorId(id));
  }

  @Operation(summary = "Actualizar donación")
  @PutMapping("/{id}")
  public ResponseEntity<DonacionDTO> actualizarDonacion(@PathVariable UUID id, @RequestBody DonacionDTO dto) {
    return ResponseEntity.ok(donacionService.actualizarDonacion(id, dto));
  }

  @Operation(summary = "Eliminar donación")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonacion(@PathVariable UUID id) {
    donacionService.eliminarDonacion(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Cambiar estado de una donación")
  @PatchMapping("/{id}/estado")
  public ResponseEntity<DonacionDTO> cambiarEstado(@PathVariable UUID id, @RequestBody CambioEstadoDTO cambioEstadoDTO) {
    return ResponseEntity.ok(donacionService.cambiarEstado(
        id,
        cambioEstadoDTO.getNuevoEstado(),
        cambioEstadoDTO.getJustificacion()
    ));
  }

  @Operation(summary = "Marcar donación como vencida (Solo Admins)")
  @PatchMapping("/{id}/vencer")
  public ResponseEntity<DonacionDTO> marcarComoVencida(@PathVariable UUID id) {
    return ResponseEntity.ok(donacionService.marcarComoVencida(id));
  }

  @Operation(summary = "Ver resultados de matchmaking pendientes")
  @GetMapping("/pendientes")
  public ResponseEntity<List<ResultadoMatchmakingDTO>> verResultadosDeMatchmaking() {
    return ResponseEntity.ok(donacionService.obtenerTodosLosResultadosMatchmaking());
  }

  @Operation(summary = "Ejecutar algoritmos de asignación a demanda")
  @PostMapping("/matchmaking/ejecutar")
  public ResponseEntity<Void> ejecutarMatchmaking() {
    donacionService.ejecutarMatchmakingADemanda();
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Aprobar asignación de donación")
  @PostMapping("/asignar")
  public ResponseEntity<Void> asignarPropuesta(@RequestBody AsignarPropuestaRequestDTO request) {
    try {
      donacionService.asignarPropuesta(
          request.getDonacionId(),
          request.getPosicionPropuesta()
      );
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}