package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.AsignarPropuestaRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.CambioEstadoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/donaciones")
@Tag(name = "Servicio de donaciones", description = "Endpoints para operaciones CRUD de Donaciones")
public class DonacionController {

  private final DonacionService donacionService;

  public DonacionController(DonacionService donacionService) {
    this.donacionService = donacionService;
  }

  @Operation(summary = "Crear una Donacion")
  @PostMapping("/formulario")
  public ResponseEntity<List<DonacionDTO>> crearDonacion(@RequestBody FormularioRequestDTO request) {
    List<DonacionDTO> donaciones = donacionService.procesarFormulario(request);
    if (donaciones == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(donaciones);
  }

  @Operation(summary = "Ver donaciones")
  @GetMapping
  public ResponseEntity<List<DonacionDTO>> obtenerDonaciones() {
    return ResponseEntity.ok(donacionService.obtenerTodas());
  }

  @Operation(summary = "Ver donacion por id")
  @GetMapping("/{id}")
  public ResponseEntity<DonacionDTO> obtenerDonacion(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(donacionService.obtenerPorId(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "Actualizar donacion")
  @PutMapping("/{id}")
  public ResponseEntity<DonacionDTO> actualizarDonacion(@PathVariable UUID id, @RequestBody DonacionDTO dto) {
    try {
      return ResponseEntity.ok(donacionService.actualizarDonacion(id, dto));
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "Eliminar donacion")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonacion(@PathVariable UUID id) {
    donacionService.eliminarDonacion(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Cambiar estado de una donacion")
  @PatchMapping("/{id}/estado")
  public ResponseEntity<DonacionDTO> cambiarEstado(@PathVariable UUID id, @RequestBody CambioEstadoDTO cambioEstadoDTO) {
    try {
      return ResponseEntity.ok(donacionService.cambiarEstado(
          id,
          cambioEstadoDTO.getNuevoEstado(),
          cambioEstadoDTO.getJustificacion()
      ));
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "Marcar donacion como vencida (Solo Admins)")
  @PatchMapping("/{id}/vencer")
  public ResponseEntity<DonacionDTO> marcarComoVencida(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(donacionService.marcarComoVencida(id));
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "Ver resultados de matchmaking pendientes")
  @GetMapping("/pendientes")
  public ResponseEntity<List<ResultadoMatchmakingDTO>> verResultadosDeMatchmaking() {
    return ResponseEntity.ok(donacionService.obtenerTodosLosResultadosMatchmaking());
  }

  @Operation(summary = "Ejecutar algoritmos de asignacion a demanda")
  @PostMapping("/matchmaking/ejecutar")
  public ResponseEntity<Void> ejecutarMatchmaking() {
    donacionService.ejecutarMatchmakingADemanda();
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Aprobar asignacion de donacion")
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