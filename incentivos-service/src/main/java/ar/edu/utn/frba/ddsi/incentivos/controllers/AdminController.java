package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.AdminService;
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
@RequestMapping("/api/admin")
@Tag(name = "Administración", description = "Endpoints para administradores - gestión de categorías y misiones del sistema gamificado.")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    // ========== CATEGORÍAS - GET ==========
    @Operation(
        summary = "Obtener todas las categorías",
        description = "Retorna la lista completa de categorías disponibles en el sistema, ordenadas por secuencia."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categorías obtenidas con éxito"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> obtenerCategorias(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin) {
        List<CategoriaDTO> categorias = service.obtenerCategorias(idAdmin);
        return ResponseEntity.ok(categorias);
    }

    // TODO: PAGINACION

    @Operation(
        summary = "Obtener una categoría específica",
        description = "Retorna los detalles de una categoría identificada por su UUID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categoría obtenida con éxito"),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTO> obtenerCategoriaPorId(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin,
        @Parameter(description = "UUID de la categoría")
        @PathVariable UUID id) {
        CategoriaDTO categoria = service.obtenerCategoriaPorId(idAdmin, id);
        return categoria == null
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(categoria);
    }

    // ========== CATEGORÍAS - CREATE ==========
    @Operation(
        summary = "Crear una nueva categoría",
        description = "Crea una nueva categoría en el sistema y la agrega a la secuencia."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Categoría creada con éxito"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PostMapping("/categorias")
    public ResponseEntity<CategoriaDTO> crearCategoria(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin,
        @RequestBody CategoriaDTO request) {
        CategoriaDTO nuevaCategoria = service.agregarCategoria(idAdmin, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaCategoria);
    }

    // ========== CATEGORÍAS - UPDATE ==========
    @Operation(
        summary = "Actualizar una categoría",
        description = "Actualiza los datos de una categoría existente."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categoría actualizada con éxito"),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTO> actualizarCategoria(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin,
        @Parameter(description = "UUID de la categoría a actualizar")
        @PathVariable UUID id,
        @RequestBody CategoriaDTO categoria) {
        CategoriaDTO actualizada = service.actualizarCategoria(idAdmin, id, categoria);
        return actualizada == null
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(actualizada);
    }

    // ========== CATEGORÍAS - DELETE ==========
    @Operation(
        summary = "Eliminar una categoría",
        description = "Elimina una categoría del sistema y reordena la secuencia."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categoría eliminada con éxito"),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<List<CategoriaDTO>> eliminarCategoria(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin,
        @Parameter(description = "UUID de la categoría a eliminar")
        @PathVariable UUID id) {
        List<CategoriaDTO> nuevaSecuencia = service.eliminarCategoria(idAdmin, id);
        return ResponseEntity.ok(nuevaSecuencia);
    }

    // ========== MISIONES - GET ==========
    @Operation(
        summary = "Obtener todas las misiones",
        description = "Retorna la lista completa de misiones disponibles en el sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Misiones obtenidas con éxito"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/misiones")
    public ResponseEntity<List<MisionDTO>> obtenerMisiones(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin) {
        List<MisionDTO> misiones = service.obtenerMisiones(idAdmin);
        return ResponseEntity.ok(misiones);
    }

    // TODO: PAGINACION

    @Operation(
        summary = "Obtener una misión específica",
        description = "Retorna los detalles de una misión identificada por su UUID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Misión obtenida con éxito"),
        @ApiResponse(responseCode = "404", description = "Misión no encontrada"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/misiones/{id}")
    public ResponseEntity<MisionDTO> obtenerMisionPorId(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin,
        @Parameter(description = "UUID de la misión")
        @PathVariable UUID id) {
        MisionDTO mision = service.obtenerMisionPorId(idAdmin, id);
        return mision == null
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(mision);
    }

    // ========== MISIONES - CREATE ==========
    @Operation(
        summary = "Crear una nueva misión",
        description = "Crea una nueva misión en el sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Misión creada con éxito"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PostMapping("/misiones")
    public ResponseEntity<MisionDTO> crearMision(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin,
        @RequestBody MisionDTO request) {
        MisionDTO nuevaMision = service.crearMision(idAdmin, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaMision);
    }

    // ========== MISIONES - UPDATE ==========
    @Operation(
        summary = "Actualizar una misión",
        description = "Actualiza los datos de una misión existente."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Misión actualizada con éxito"),
        @ApiResponse(responseCode = "404", description = "Misión no encontrada"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PutMapping("/misiones/{id}")
    public ResponseEntity<MisionDTO> actualizarMision(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin,
        @Parameter(description = "UUID de la misión a actualizar")
        @PathVariable UUID id,
        @RequestBody MisionDTO mision) {
        MisionDTO actualizada = service.actualizarMision(idAdmin, id, mision);
        return actualizada == null
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(actualizada);
    }

    // ========== MISIONES - DELETE ==========
    @Operation(
        summary = "Eliminar una misión",
        description = "Elimina una misión del sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Misión eliminada con éxito"),
        @ApiResponse(responseCode = "404", description = "Misión no encontrada"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @DeleteMapping("/misiones/{id}")
    public ResponseEntity<MisionDTO> eliminarMision(
        @Parameter(description = "UUID del administrador")
        @RequestHeader("Admin-Id") UUID idAdmin,
        @Parameter(description = "UUID de la misión a eliminar")
        @PathVariable UUID id) {
        MisionDTO misionEliminada = service.eliminarMision(idAdmin, id);
        return misionEliminada == null
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(misionEliminada);
    }
}


/*
Sos admin?
⣶⣿⣾⣷⣷⣾⣿⣿⣿⠋⠀⠀⠀⢠⣾⣿⣿⣿⣶⣤⣤⡀⠀⠀⢀⠀⢹⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⡏⠀⠀⠀⢰⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣶⣿⣷⣸⣿⣿⣿
⣿⣿⣿⣿⣿⣿⡿⡟⠀⠀⠀⠀⢿⣿⣿⣿⢷⣿⣿⣿⣿⣿⣿⣿⠿⠁⠃⣿⣿⣿
⣿⣿⣿⣿⣿⣿⡄⡘⣤⠀⢀⣾⣿⣻⡿⢿⣿⣿⣿⣿⣿⣿⣿⣏⠀⠀⠀⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⡉⣩⠀⣦⡙⠟⠟⠛⢓⠀⠙⢿⣿⣿⣿⣿⡧⠀⠀⣾⣿⣿⣿
⣿⣿⣿⣿⣿⣿⢿⣯⣿⢸⣿⣷⡗⠀⠀⠁⠒⠠⢀⢰⡿⠻⠼⠓⠂⠘⢿⣿⣿⣿
⣻⣿⡟⣯⣿⢋⣯⡟⣿⠏⠿⣿⣧⠀⠀⠀⠀⠀⠀⣨⡇⠀⠀⠀⠀⠀⣈⣿⣿⣿
⣿⣿⠿⣟⣻⣿⣿⣇⡟⡀⠀⣹⣿⣷⣦⣤⣤⣦⣾⣿⠇⠀⠀⠀⠀⢀⣾⣿⣿⣿
⣿⠃⠐⢩⣿⣿⢟⠟⠃⠀⠀⢸⣿⣿⣿⣿⡟⠿⢿⣿⡄⢰⡆⠀⢀⣿⣿⣿⣿⣿
⡏⡄⠀⠀⢻⡁⠀⠀⠀⠀⠐⠅⢻⣿⣿⣿⣿⣶⡄⠀⠀⠀⠁⢀⣾⣿⣿⣿⣿⣿
⠔⠀⠀⠀⠈⢟⢄⠀⠀⠀⠀⠈⠘⠛⢷⡄⠀⠭⠉⠁⠀⠀⢠⣾⣿⣿⣿⣿⣿⣿
⢵⠀⠀⠀⠀⠈⢉⣦⠀⠀⠀⠀⠀⢱⣶⣿⣶⣶⡄⠀⠀⣴⣿⣿⣿⣿⣿⣿⣿⣿
⢀⡐⠀⠀⠀⠀⠈⢼⢅⠀⠀⠀⠀⠀⠈⠉⠛⠛⠁⣠⣾⣿⣿⣿⣿⣿⣿⣿⣿⣿
⠐⠀⠠⡀⠀⠀⠀⠈⠛⢦⡀⠀⠀⠀⠀⠀⠀⠀⠀⠈⢣⠉⠉⠉⠙⠻⠿⠿⣿⣿
⠓⠄⡀⠈⠢⠀⠀⠐⢤⣄⡀⠀⠀⠀⠀⠰⠀⠀⠀⠀⠀⠣⡄⠀⠀⠀⠀⠀⠀⠀
*/


