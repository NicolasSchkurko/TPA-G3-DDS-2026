package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.controllers.request.CategoriaFiltroRequest;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.AdminService;
import ar.edu.utn.frba.ddsi.incentivos.services.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/categorias")
@Tag(name = "Administración - Categorías", description = "Gestión de categorías y su secuencia en el sistema gamificado.")
public class CategoriaController {

  private final CategoriaService service;

  public CategoriaController(CategoriaService service) {
    this.service = service;
  }

  @Operation(
      summary = "Obtener todas las categorías",
      description = "Retorna la lista paginada de categorías disponibles en el sistema, filtrable y ordenada por posición de secuencia."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Categorías obtenidas con éxito"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @GetMapping
  public ResponseEntity<Page<CategoriaDTO>> obtenerCategorias(
      @ParameterObject @ModelAttribute CategoriaFiltroRequest filtros,

      @ParameterObject
      @PageableDefault(page = 0, size = 10, sort = "posicionSecuencia", direction = Sort.Direction.ASC)
      Pageable pageable
  ) {
    return ResponseEntity.ok(service.obtenerCategorias(filtros, pageable));
  }

  @Operation(
      summary = "Obtener una categoría específica",
      description = "Retorna los detalles de una categoría identificada por su UUID."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Categoría obtenida con éxito"),
      @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @GetMapping("/{id}")
  public ResponseEntity<CategoriaDTO> obtenerCategoriaPorId(
      @Parameter(description = "UUID de la categoría", required = true)
      @PathVariable UUID id
  ) {
    CategoriaDTO categoria = service.obtenerCategoriaPorId(id);
    return categoria == null
           ? ResponseEntity.notFound().build()
           : ResponseEntity.ok(categoria);
  }

  @Operation(
      summary = "Crear una nueva categoría",
      description = "Crea una nueva categoría en el sistema y la agrega a la secuencia."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Categoría creada con éxito"),
      @ApiResponse(responseCode = "400", description = "Datos inválidos"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @PostMapping("/admin")
  public ResponseEntity<CategoriaDTO> crearCategoria(
      @Parameter(description = "UUID del administrador", required = true)
      @RequestHeader("Admin-Id") UUID idAdmin,
      @RequestBody CategoriaDTO request
  ) {
    CategoriaDTO nuevaCategoria = service.agregarCategoria(idAdmin, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(nuevaCategoria);
  }

  @Operation(
      summary = "Actualizar una categoría",
      description = "Actualiza los datos de una categoría existente."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Categoría actualizada con éxito"),
      @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
      @ApiResponse(responseCode = "403", description = "No autorizado")
  })
  @PutMapping("/admin/{id}")
  public ResponseEntity<CategoriaDTO> actualizarCategoria(
      @Parameter(description = "UUID del administrador", required = true)
      @RequestHeader("Admin-Id") UUID idAdmin,
      @Parameter(description = "UUID de la categoría a actualizar", required = true)
      @PathVariable UUID id,
      @RequestBody CategoriaDTO categoria
  ) {
    CategoriaDTO actualizada = service.actualizarCategoria(idAdmin, id, categoria);
    return actualizada == null
           ? ResponseEntity.notFound().build()
           : ResponseEntity.ok(actualizada);
  }

  @Operation(
      summary = "Eliminar una categoría",
      description = "Elimina una categoría existente y reacomoda la secuencia de las restantes."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Categoría eliminada con éxito"),
      @ApiResponse(responseCode = "403", description = "No autorizado"),
      @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
  })
  @DeleteMapping("/admin/{id}")
  public ResponseEntity<Void> eliminarCategoria(
      @Parameter(description = "UUID del administrador", required = true)
      @RequestHeader("Admin-Id") UUID idAdmin,
      @Parameter(description = "UUID de la categoría a eliminar", required = true)
      @PathVariable UUID id
  ) {
    service.eliminarCategoria(idAdmin, id);
    return ResponseEntity.noContent().build();
  }
}