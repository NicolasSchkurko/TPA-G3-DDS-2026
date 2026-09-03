package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
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
@Tag(name = "Servicio de entidades beneficiarias", description = "Endpoints para operaciones CRUD de Entidades beneficiarias y la administracion de sus necesidades")
@RequestMapping("/entidades")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaService service;

    public EntidadBeneficiariaController(EntidadBeneficiariaService service) {
        this.service = service;
    }

    // --- ENDPOINTS DE ENTIDAD BENEFICIARIA ---

    @Operation(summary = "Ver entidades", description = "permite ver todas las entidades del repositorio de entidades")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "entidades obtenidas con exito"),
        @ApiResponse(responseCode = "400", description = "Error al tratar de obtener las entidades")
    })
    @GetMapping
    public ResponseEntity<List<EntidadBeneficiariaDTO>> obtenerTodas() {
        return ResponseEntity.ok(service.obtenerTodas());
    }

    @Operation(summary = "Ver entidad por id", description = "permite buscar y obtener una entidad en base a su id")
    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiariaDTO> obtenerEntidad(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.obtenerEntidadPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Crear una Entidad", description = "permite registrar una nueva entidad")
    @PostMapping
    public ResponseEntity<?> registrarEntidad(@RequestBody EntidadBeneficiariaDTO dto) {
        try {
            EntidadBeneficiariaDTO registrada = service.registrarEntidad(dto);
            return new ResponseEntity<>(registrada, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "actualizar entidad", description = "permite actualizar una entidad buscandola por su id")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarEntidad(@PathVariable UUID id, @RequestBody EntidadBeneficiariaDTO dto) {
        try {
            EntidadBeneficiariaDTO actualizada = service.actualizarEntidad(id, dto);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "eliminar entidad")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEntidad(@PathVariable UUID id) {
        service.eliminarEntidad(id);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINTS DE NECESIDADES ---

    @Operation(summary = "Ver necesidades")
    @GetMapping("/{id}/necesidades")
    public ResponseEntity<List<NecesidadDTO>> obtenerNecesidades(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.obtenerNecesidades(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "agregar necesidad")
    @PostMapping("/{id}/necesidades")
    public ResponseEntity<?> agregarNecesidad(@PathVariable UUID id, @RequestBody NecesidadDTO dto) {
        try {
            NecesidadDTO agregada = service.agregarNecesidad(id, dto);
            return new ResponseEntity<>(agregada, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "actualizar necesidad", description = "permite actualizar una necesidad buscandola por su id")
    @PutMapping("/{id}/necesidades/{idNecesidad}")
    public ResponseEntity<?> actualizarNecesidad(@PathVariable UUID id, @PathVariable UUID idNecesidad, @RequestBody NecesidadDTO dto) {
        try {
            NecesidadDTO actualizada = service.actualizarNecesidad(idNecesidad, dto);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "eliminar necesidad")
    @DeleteMapping("/{id}/necesidades/{idNecesidad}")
    public ResponseEntity<Void> eliminarNecesidad(@PathVariable UUID id, @PathVariable UUID idNecesidad) {
        try {
            service.eliminarNecesidad(id, idNecesidad);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- ENDPOINTS DE DONACIONES DE LA ENTIDAD ---

    @Operation(summary = "Ver donaciones")
    @GetMapping("/{id}/donaciones")
    public ResponseEntity<List<DonacionDTO>> obtenerDonaciones(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.obtenerDonaciones(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}