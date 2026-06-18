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

  // CREATE (C) - Recibe la entidad (o podrías crear un DonacionCreateDTO) y devuelve el DTO
  @PostMapping
  public ResponseEntity<DonacionDTO> crearDonacion(@RequestBody Donacion donacion) {
    DonacionDTO nuevaDonacion = donacionService.crearDonacion(donacion);
    return new ResponseEntity<>(nuevaDonacion, HttpStatus.CREATED);
  }

  // CREATE (C) - Endpoint que procesa el formulario completo y segmenta
  @PostMapping("/formulario")
  public ResponseEntity<List<DonacionDTO>> procesarFormulario(@RequestBody FormularioRequestDTO request) {
    List<DonacionDTO> donacionesSegmentadas = donacionService.procesarFormulario(
        request.getDonante(),
        request.getBienes(),
        request.getFechaRealizacion()
    );
    // Devuelve un array con todas las donaciones individuales creadas a partir del formulario
    return new ResponseEntity<>(donacionesSegmentadas, HttpStatus.CREATED);
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
      DonacionDTO actualizada = donacionService.actualizarDonacion(id, donacion);
      return ResponseEntity.ok(actualizada);
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
      DonacionDTO donacionActualizada = donacionService.cambiarEstado(
          id,
          cambioEstadoDTO.getNuevoEstado(),
          cambioEstadoDTO.getJustificacion()
      );
      return ResponseEntity.ok(donacionActualizada);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }
}