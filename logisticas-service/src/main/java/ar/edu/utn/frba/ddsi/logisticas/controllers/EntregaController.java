package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.ActualizacionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.PeticionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/entregas")
@Tag(name = "Entregas", description = "API CRUD y gestión de trazabilidad de los paquetes a entregar")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    // --- CRUD ---

    @Operation(summary = "Listar todos los ítems de entrega del depósito")
    @GetMapping
    public ResponseEntity<List<ItemEntrega>> obtenerTodas() {
        return ResponseEntity.ok(entregaService.findAll());
    }

    @Operation(summary = "Listar ítems de entrega NO_RECIBIDA pendientes de revisión")
    @GetMapping("/no-recibidas")
    public ResponseEntity<List<ItemEntrega>> obtenerNoRecibidas() {
        return ResponseEntity.ok(entregaService.obtenerEntregasNoRecibidas());
    }

    @Operation(summary = "Obtener detalle de un ítem de entrega por su ID de Donación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item encontrado"),
            @ApiResponse(responseCode = "404", description = "Item no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(entregaService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Eliminar un ítem de entrega del sistema logístico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item eliminado"),
            @ApiResponse(responseCode = "404", description = "Item no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarEntrega(@PathVariable UUID id) {
        try {
            entregaService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // --- OPERACIONES DE NEGOCIO ---

    @Operation(summary = "Registrar nuevas entregas a distribuir",
        description = "Recibe el payload desde el módulo de donaciones y crea los ítems pendientes.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ítems creados y registrados en depósito.")
    })
    @PostMapping
    public ResponseEntity<String> crearDestinos(@RequestBody PeticionEntregaDTO request) {
        try {
            entregaService.procesarPeticion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("Ítems de entrega agregados al depósito correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar la petición: " + e.getMessage());
        }
    }

    @Operation(summary = "Trazabilidad: Actualizar estado de una entrega (Recepción, Rechazo, Reingreso)",
        description = "Permite confirmar (requiere URL de foto), rechazar (requiere justificación) " +
            "o reingresar a depósito una entrega NO_RECIBIDA tras revisión (estado PENDIENTE).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado modificado exitosamente."),
        @ApiResponse(responseCode = "400", description = "Payload inválido o falta de foto/justificación obligatoria.")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<String> actualizarEstadoEntrega(
        @PathVariable UUID id,
        @RequestBody ActualizacionEntregaDTO request) {
        try {
            entregaService.actualizarEstado(id, request);
            return ResponseEntity.ok("Estado de la entrega actualizado correctamente a: " + request.getEstado());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + e.getMessage());
        }
    }
}