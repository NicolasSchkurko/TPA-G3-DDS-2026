package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.CambioEstadoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/donaciones")
public class DonacionController {

  private final DonacionService donacionService;

  public DonacionController(DonacionService donacionService) {
    this.donacionService = donacionService;
  }

  // CREATE (C) - Endpoint que procesa el formulario completo y segmentar
  @PostMapping("/formulario")
  public ResponseEntity<List<DonacionDTO>> crearDonacion(@RequestBody FormularioRequestDTO request) {
    List<Donacion> donacionesSegmentadas = donacionService.procesarFormulario(
        request.getDonante(),
        request.getBienes(),
        request.getFechaRealizacion()
    );
    if (donacionesSegmentadas == null) {
      return ResponseEntity.notFound().build();
    }

    List<DonacionDTO> dto = donacionesSegmentadas.stream()
            .map(donacionService::toDTO)
            .toList();

    return ResponseEntity.ok(dto);
  }

  // READ (R) - Devuelve lista de DTOs
  @GetMapping
  public ResponseEntity<List<DonacionDTO>> obtenerDonaciones() {
    List<DonacionDTO> donaciones = donacionService.obtenerTodas();
    return ResponseEntity.ok(donaciones);
  }

  // READ (R) - Devuelve un DTO
  @GetMapping("/{id}")
  public ResponseEntity<DonacionDTO> obtenerDonacion(@PathVariable UUID id) {
    Optional<DonacionDTO> donacion = donacionService.obtenerPorId(id);
    return donacion.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
  }

  // UPDATE (U) - Devuelve DTO
  @PutMapping("/{id}")
  public ResponseEntity<DonacionDTO> actualizarDonacion(@PathVariable UUID id, @RequestBody Donacion donacion) {
    try {
      Donacion actualizada = donacionService.actualizarDonacion(id, donacion);
      return ResponseEntity.ok(donacionService.toDTO(actualizada));
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  // DELETE (D)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonacion(@PathVariable UUID id) {
    donacionService.eliminarDonacion(id);
    return ResponseEntity.noContent().build();
  }

  // PATCH - Devuelve DTO
  @PatchMapping("/{id}/estado")
  public ResponseEntity<DonacionDTO> cambiarEstado(@PathVariable UUID id, @RequestBody CambioEstadoDTO cambioEstadoDTO) {
    try {
      Donacion donacionActualizada = donacionService.cambiarEstado(
          id,
          cambioEstadoDTO.getNuevoEstado(),
          cambioEstadoDTO.getJustificacion()
      );
      return ResponseEntity.ok(donacionService.toDTO(donacionActualizada));
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
