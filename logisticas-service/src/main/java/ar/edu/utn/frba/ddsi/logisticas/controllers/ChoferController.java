package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferesDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.ChoferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/choferes")
@Tag(name = "Choferes", description = "API CRUD y gestión de estados operativos de los Choferes")
public class ChoferController {

    private final ChoferService choferService;

    public ChoferController(ChoferService choferService) {
        this.choferService = choferService;
    }

    // --- CRUD ---

    @Operation(summary = "Listar choferes activos de forma paginada")
    @GetMapping
    public ResponseEntity<Page<ChoferDTO>> obtenerTodos(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(choferService.findAll(pageable));
    }

    @Operation(summary = "Obtener un chofer por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chofer encontrado"),
            @ApiResponse(responseCode = "404", description = "Chofer no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(choferService.findById(id));
    }

    @Operation(summary = "Registrar un nuevo chofer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chofer registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos del chofer inválidos"),
            @ApiResponse(responseCode = "409", description = "El chofer ya se encuentra registrado")
    })
    @PostMapping
    public ResponseEntity<?> crearChofer(@Valid @RequestBody ChoferDTO request) {
        ChoferDTO nuevo = choferService.create(request);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }

    @Operation(summary = "Registrar múltiples choferes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Choferes registrados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de choferes inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflicto por clave duplicada")
    })
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarChoferes(@Valid @RequestBody ChoferesDTO choferes) {
        ChoferesDTO nuevosChoferes = choferService.createMultiple(choferes);
        return new ResponseEntity<>(nuevosChoferes, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar datos de un chofer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chofer actualizado"),
            @ApiResponse(responseCode = "404", description = "Chofer no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarChofer(@PathVariable UUID id, @RequestBody ChoferDTO request) {
        return ResponseEntity.ok(choferService.update(id, request));
    }

    @Operation(summary = "Eliminar un chofer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chofer eliminado"),
            @ApiResponse(responseCode = "404", description = "Chofer no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarChofer(@PathVariable UUID id) {
        choferService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- OPERACIONES DE NEGOCIO ---

    @Operation(summary = "Actualizar la disponibilidad de un chofer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Falta el campo 'disponible'"),
        @ApiResponse(responseCode = "404", description = "Chofer no encontrado")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<String> cambiarEstadoDisponibilidad(
        @PathVariable UUID id,
        @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(choferService.cambiarDisponibilidad(id, body));
    }
}
