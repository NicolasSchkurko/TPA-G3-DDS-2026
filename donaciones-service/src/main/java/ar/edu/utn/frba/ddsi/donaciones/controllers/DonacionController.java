package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.AsignarPropuestaRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.CambioEstadoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.mappers.DonacionMapper;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/donaciones")
@Tag(name = "Servicio de donaciones", description = "Endpoints para operaciones CRUD de Donaciones ademas cambiar su estado y visualizar y asignar donaciones pendientes")
public class DonacionController {

  private final DonacionService donacionService;
  private final DonacionMapper donacionMapper;

  public DonacionController(DonacionService donacionService,
                            DonacionMapper donacionMapper) {
    this.donacionService = donacionService;
    this.donacionMapper = donacionMapper;
  }

  // CREATE (C) - Endpoint que procesa el formulario completo y segmentar
  @Operation(
          summary = "Crear una Donacion",
          description = "permite procesar un formulario de donacion y lo segmenta en donaciones individuales para despues asignar"
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "202", description = "Formulario procesado con exito y donaciones creadas"),
          @ApiResponse(responseCode = "400", description = "Error al Procear formulario o crear las donaciones")
  })
  @PostMapping("/formulario")
  public ResponseEntity<List<DonacionDTO>> crearDonacion(@RequestBody FormularioRequestDTO request) {
    List<Donacion> donacionesSegmentadas = donacionService.procesarFormulario(
        request.getIdDonante(),
        request.getBienes(),
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

  @Operation(
          summary = "Ver donaciones",
          description = "permite ver todas las donaciones del repositorio de donaciones"
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "202", description = "donaciones obtenidas con exito"),
          @ApiResponse(responseCode = "400", description = "Error al tratar de obtener las donaciones")
  })
  // READ (R) - Devuelve lista de DTOs
  @GetMapping
  public ResponseEntity<List<DonacionDTO>> obtenerDonaciones() {
    List<DonacionDTO> donaciones = donacionService.obtenerTodas();
    return ResponseEntity.ok(donaciones);
  }

  @Operation(
          summary = "Ver donacion por id",
          description = "permite buscar y obtener una donacion en base a su id"
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "202", description = "donacion obtenida con exito"),
          @ApiResponse(responseCode = "400", description = "Error al tratar de obtener la donacion")
  })
  // READ (R) - Devuelve un DTO
  @GetMapping("/{id}")
  public ResponseEntity<DonacionDTO> obtenerDonacion(@PathVariable UUID id) {
    Optional<DonacionDTO> donacion = donacionService.obtenerPorId(id);
    return donacion.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(
          summary = "actualizar donacion",
          description = "permite actualizar una donacion buscandola por su id y cargando la nueva donacion con los datos actualizados"
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "202", description = "donacion actualizada con exito"),
          @ApiResponse(responseCode = "400", description = "Error al tratar de actualizar la donacion")
  })
  // UPDATE (U) - Devuelve DTO
  @PutMapping("/{id}")
  public ResponseEntity<DonacionDTO> actualizarDonacion(@PathVariable UUID id, @RequestBody DonacionDTO donacion) {
    try {
      Donacion actualizada = donacionService.actualizarDonacion(id, donacion);
      return ResponseEntity.ok(donacionMapper.donaciontoDTO(actualizada));
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(
          summary = "eliminar donacion",
          description = "permite eliminar una donacion buscandola por su id"
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "202", description = "donacion eliminada con exito"),
          @ApiResponse(responseCode = "400", description = "Error al tratar de eliminar la donacion")
  })
  // DELETE (D)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonacion(@PathVariable UUID id) {
    donacionService.eliminarDonacion(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
          summary = "cambiar estado de una donacion",
          description = "en base a la id de la donacion, se permite cambiar su estado junto con la justificacion de su cambio"
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "202", description = "cambio de estado de la donacion realizado con exito"),
          @ApiResponse(responseCode = "400", description = "Error al tratar de cambiar estado de la donacion")
  })
  // PATCH - Devuelve DTO
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

  @Operation(
          summary = "ver donaciones pendientes de asignacion junto con sus entidades propuestas",
          description = "permite ver los resultados de matchmaking de las donaciones pendientes de aprobacion para asignarlas a una entidad"
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "202", description = "resultados de matchmaking para cada donacion obtenidos con exito"),
          @ApiResponse(responseCode = "400", description = "Error al obtener los resultados de matchmaking para las donaciones pendientes de asignacion")
  })
  @GetMapping("/pendientes")
  public ResponseEntity<List<ResultadoMatchmakingDTO>> verResultadosDeMatchmaking() {

    List<ResultadoMatchmakingDTO> resultados =
            donacionService.obtenerTodosLosResultadosMatchmaking();

    return ResponseEntity.ok(resultados);
  }

  @Operation(
          summary = "aprobar asignacion de donacion",
          description = "permite aprobar la asignacion de una donacion a una de las entidades propuestas"
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "202", description = "donacion aprobada y asignada con exito"),
          @ApiResponse(responseCode = "400", description = "Error al tratar de aprobar y asignar donacion")
  })
  @PostMapping("/asignar")
  public ResponseEntity<Void> asignarPropuesta(
          @RequestBody AsignarPropuestaRequestDTO request) {

    donacionService.asignarPropuesta(
            request.getDonacionId(),
            request.getPosicionPropuesta()
    );

    return ResponseEntity.ok().build();
  }

}
