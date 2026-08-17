package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.AsignarPropuestaRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.CambioEstadoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.mappers.DonacionMapper;
import ar.edu.utn.frba.ddsi.donaciones.mappers.MatchmakingMapper;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/donaciones")
@Tag(name = "Servicio de donaciones", description = "Endpoints para operaciones CRUD de Donaciones")
public class DonacionController {

  private final DonacionService donacionService;
  private final DonacionMapper donacionMapper;
  private final MatchmakingMapper mapperMatchmaking;

  public DonacionController(DonacionService donacionService,
                            DonacionMapper donacionMapper,
                            MatchmakingMapper mapperMatchmaking) {
    this.donacionService = donacionService;
    this.donacionMapper = donacionMapper;
    this.mapperMatchmaking = mapperMatchmaking;
  }

  @Operation(summary = "Crear una Donacion")
  @PostMapping("/formulario")
  public ResponseEntity<List<DonacionDTO>> crearDonacion(@RequestBody FormularioRequestDTO request) {
    List<Bien> bienesDeDominio = maptodto(request.getBienes());
    List<Donacion> donacionesSegmentadas = donacionService.procesarFormulario(
        request.getIdDonante(),
        bienesDeDominio,
        request.getFechaRealizacion()
    );
    if (donacionesSegmentadas == null) {
      return ResponseEntity.notFound().build();
    }

    List<DonacionDTO> dto = donacionesSegmentadas.stream()
                                                 .map(donacionMapper::donaciontoDTO)
                                                 .toList();

    return ResponseEntity.ok(dto);
  }

  @Operation(summary = "Ver donaciones")
  @GetMapping
  public ResponseEntity<List<DonacionDTO>> obtenerDonaciones() {
    List<DonacionDTO> donaciones = donacionService.obtenerTodas().stream()
                                                  .map(donacionMapper::donaciontoDTO)
                                                  .collect(Collectors.toList());
    return ResponseEntity.ok(donaciones);
  }

  @Operation(summary = "Ver donacion por id")
  @GetMapping("/{id}")
  public ResponseEntity<DonacionDTO> obtenerDonacion(@PathVariable UUID id) {
    Optional<Donacion> donacion = donacionService.obtenerPorId(id);
    return donacion.map(d -> ResponseEntity.ok(donacionMapper.donaciontoDTO(d)))
                   .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(summary = "actualizar donacion")
  @PutMapping("/{id}")
  public ResponseEntity<DonacionDTO> actualizarDonacion(@PathVariable UUID id, @RequestBody DonacionDTO dto) {
    try {
      // Requiere que donacionService actualice la entidad basada en un DTO o que el controlador la mapee antes.
      // Para simplificar manteniendo la estructura de mapeo previa:
      Donacion actualizada = donacionService.actualizarDonacion(id, donacionMapper.dtoToDonacion(dto));
      return ResponseEntity.ok(donacionMapper.donaciontoDTO(actualizada));
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "eliminar donacion")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonacion(@PathVariable UUID id) {
    donacionService.eliminarDonacion(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "cambiar estado de una donacion")
  @PatchMapping("/{id}/estado")
  public ResponseEntity<DonacionDTO> cambiarEstado(@PathVariable UUID id, @RequestBody CambioEstadoDTO cambioEstadoDTO) {
    try {
      Donacion donacionActualizada = donacionService.cambiarEstado(
          id,
          cambioEstadoDTO.getNuevoEstado(),
          cambioEstadoDTO.getJustificacion()
      );
      return ResponseEntity.ok(donacionMapper.donaciontoDTO(donacionActualizada));
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "ver donaciones pendientes")
  @GetMapping("/pendientes")
  public ResponseEntity<List<ResultadoMatchmakingDTO>> verResultadosDeMatchmaking() {
    List<ResultadoMatchmakingDTO> resultados = donacionService.obtenerTodosLosResultadosMatchmaking()
                                                              .stream()
                                                              .map(mapperMatchmaking::ResultadoToDTO)
                                                              .toList();
    return ResponseEntity.ok(resultados);
  }

  @Operation(summary = "aprobar asignacion de donacion")
  @PostMapping("/asignar")
  public ResponseEntity<Void> asignarPropuesta(@RequestBody AsignarPropuestaRequestDTO request) {
    donacionService.asignarPropuesta(
        request.getDonacionId(),
        request.getPosicionPropuesta()
    );
    return ResponseEntity.ok().build();
  }

  private List<Bien> maptodto(List<BienResumenDTO> bienes) {
    List<Bien> b = new ArrayList<>();
    for (BienResumenDTO x : bienes) {
      Bien bienNormal;
      switch (x.getTipoBien().toUpperCase()) {
        case "CON_ESTADO":
          CategoriaBien a = new CategoriaBien(x.getCategoria());
          SubcategoriaBien p = new SubcategoriaBien(x.getSubcategoria(), a);
          bienNormal = new BienConEstado(x.getDescripcion(),
                                         p,
                                         null,
                                         x.getCantidad(),
                                         mapToUM(x.getUnidadDeMedida()),
                                         x.getUsado());
          b.add(bienNormal);
          break;
        case "PERECEDERO":
          CategoriaBien d = new CategoriaBien(x.getCategoria());
          SubcategoriaBien c = new SubcategoriaBien(x.getSubcategoria(), d);
          bienNormal = new BienPerecedero(x.getDescripcion(),
                                          c,
                                          null,
                                          x.getCantidad(),
                                          mapToUM(x.getUnidadDeMedida()),
                                          x.getFechaVencimiento());
          b.add(bienNormal);
          break;
      }
    }
    return b;
  }

  private UnidadDeMedida mapToUM(String unidad) {
    return unidad.toUpperCase().equals("KILOGRAMOS") ? UnidadDeMedida.KILOGRAMOS : UnidadDeMedida.LITROS;
  }
}