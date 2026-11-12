package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.*;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @Operation(summary = "Listar de forma paginada los ítems de entrega del depósito")
    @GetMapping
    public ResponseEntity<Page<BienDTO>> obtenerTodas(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(entregaService.findAll(pageable));
    }

    @Operation(summary = "Listar ítems de entrega NO_RECIBIDA pendientes de revisión")
    @GetMapping("/no-recibidas")
    public ResponseEntity<BienesDTO> obtenerNoRecibidas() {
        return ResponseEntity.ok(entregaService.obtenerEntregasNoRecibidas());
    }

    @Operation(summary = "Obtener detalle de un ítem de entrega por su ID de Donación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item encontrado"),
            @ApiResponse(responseCode = "404", description = "Item no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(entregaService.findById(id));
    }

    // --- OPERACIONES DE NEGOCIO ---

    @Operation(summary = "Registrar nuevas entregas a distribuir",
            description = "Recibe el payload desde el módulo de donaciones y crea los ítems pendientes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ítems creados y registrados en depósito.")
    })
    @PostMapping
    public ResponseEntity<String> crearItems(
            @RequestBody EntregaDTO request,
            @RequestHeader(value = "X-Proveedor-Logistica", required = false, defaultValue = "PROPIO") String proveedor) {
        entregaService.procesarPeticion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Petición procesada exitosamente mediante el proveedor: " + proveedor);
    }

    @Operation(summary = "Trazabilidad: Actualizar estado de una entrega (Recepción, Rechazo, Reingreso)",
            description = "Permite confirmar (requiere URL de foto), rechazar (requiere justificación) " +
                    "o reingresar a depósito una entrega NO_RECIBIDA tras revisión (estado PENDIENTE).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado modificado exitosamente."),
            @ApiResponse(responseCode = "400", description = "Payload inválido o falta de foto/justificación obligatoria."),
            @ApiResponse(responseCode = "404", description = "La donación no existe."),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida.")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<String> actualizarEstadoEntrega(
            @PathVariable UUID id,
            @RequestBody ActualizacionEntregaDTO request) {
        entregaService.actualizarEstado(id, request);
        return ResponseEntity.ok("Estado de la entrega actualizado correctamente a: " + request.getEstado());
    }

    @Operation(summary = "Eliminar un ítem de entrega del sistema logístico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item eliminado"),
            @ApiResponse(responseCode = "404", description = "Item no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarEntrega(@PathVariable UUID id) {
        entregaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
