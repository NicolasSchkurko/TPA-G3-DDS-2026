package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.ActualizacionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.PeticionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/entregas")
@Tag(name = "Entregas", description = "API para la gestión de entregas y su trazabilidad")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @Operation(summary = "Registrar nuevas entregas/destinos",
        description = "Recibe los ítems listos para despachar desde el servicio de donaciones.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ítems procesados correctamente"),
        @ApiResponse(responseCode = "400", description = "El payload es inválido")
    })
    @PostMapping
    public ResponseEntity<String> crearDestinos(@RequestBody PeticionEntregaDTO request) {
        try {
            entregaService.procesarPeticion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("Ítems de entrega agregados y procesados correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar la petición: " + e.getMessage());
        }
    }

    @Operation(summary = "Actualizar estado de una entrega (Trazabilidad)",
        description = "Permite a la entidad confirmar recepción (con foto) o rechazar (con justificación). " +
            "También permite al administrador reingresar la entrega a depósito.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado actualizado y evento emitido"),
        @ApiResponse(responseCode = "400", description = "Faltan datos requeridos (foto o justificación)"),
        @ApiResponse(responseCode = "404", description = "La entrega no existe")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<String> actualizarEstadoEntrega(
        @PathVariable UUID id,
        @RequestBody ActualizacionEntregaDTO request) {
        try {
            entregaService.actualizarEstado(id, request);
            return ResponseEntity.ok("Estado de la entrega actualizado correctamente a: " + request.getEstado());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + e.getMessage());
        }
    }
}