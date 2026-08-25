package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferesDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.ChoferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "Listar todos los choferes")
    @GetMapping
    public ResponseEntity<ChoferesDTO> obtenerTodos() {
        return ResponseEntity.ok(choferService.findAll());
    }

    @Operation(summary = "Obtener un chofer por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chofer encontrado"),
            @ApiResponse(responseCode = "404", description = "Chofer no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(choferService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Registrar un nuevo chofer")
    @PostMapping
    public ResponseEntity<ChoferDTO> crearChofer(@RequestBody ChoferDTO request) {
        ChoferDTO nuevo = choferService.create(request);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar datos de un chofer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chofer actualizado"),
            @ApiResponse(responseCode = "404", description = "Chofer no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarChofer(@PathVariable UUID id, @RequestBody ChoferDTO request) {
        try {
            return ResponseEntity.ok(choferService.update(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Eliminar un chofer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chofer eliminado"),
            @ApiResponse(responseCode = "404", description = "Chofer no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarChofer(@PathVariable UUID id) {
        try {
            choferService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // --- OPERACIONES DE NEGOCIO ---

    @Operation(summary = "Actualizar la disponibilidad de un chofer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Chofer no encontrado")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<String> cambiarEstadoDisponibilidad(
        @PathVariable UUID id,
        @RequestBody Map<String, Boolean> body) {
        try {
            String mensaje = choferService.cambiarDisponibilidad(id, body);
            return ResponseEntity.ok(mensaje);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}