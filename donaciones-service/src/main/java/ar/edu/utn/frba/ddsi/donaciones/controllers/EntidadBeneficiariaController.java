package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/entidades")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaService service;

    public EntidadBeneficiariaController(EntidadBeneficiariaService service) {
        this.service = service;
    }

    // --- ENDPOINTS DE ENTIDAD BENEFICIARIA ---

    @GetMapping
    public ResponseEntity<List<EntidadBeneficiariaDTO>> obtenerTodas() {
        return ResponseEntity.ok(service.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiariaDTO> obtenerEntidad(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.obtenerEntidadPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> registrarEntidad(@RequestBody EntidadBeneficiariaDTO dto) {
        try {
            EntidadBeneficiariaDTO registrada = service.registrarEntidad(dto);
            return new ResponseEntity<>(registrada, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarEntidad(@PathVariable UUID id, @RequestBody EntidadBeneficiariaDTO dto) {
        try {
            EntidadBeneficiariaDTO actualizada = service.actualizarEntidad(id, dto);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEntidad(@PathVariable UUID id) {
        service.eliminarEntidad(id);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINTS DE NECESIDADES ---

    @GetMapping("/{id}/necesidades")
    public ResponseEntity<List<NecesidadDTO>> obtenerNecesidades(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.obtenerNecesidades(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/necesidades")
    public ResponseEntity<?> agregarNecesidad(@PathVariable UUID id, @RequestBody NecesidadDTO dto) {
        try {
            NecesidadDTO necesidad = service.agregarNecesidad(id, dto);
            return new ResponseEntity<>(necesidad, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

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

    @GetMapping("/{id}/donaciones")
    public ResponseEntity<List<DonacionDTO>> obtenerDonaciones(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.obtenerDonaciones(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}