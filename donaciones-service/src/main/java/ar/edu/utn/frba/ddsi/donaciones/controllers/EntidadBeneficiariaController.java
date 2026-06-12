package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/entidades")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaService service;

    public EntidadBeneficiariaController(EntidadBeneficiariaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EntidadBeneficiariaDTO>> obtenerTodas() {
        return ResponseEntity.ok(service.obtenerTodas());
    }

    @GetMapping("/{razonSocial}")
    public ResponseEntity<EntidadBeneficiariaDTO> obtenerEntidad(@PathVariable String razonSocial) {
        return ResponseEntity.ok(service.obtenerEntidad(razonSocial));
    }

    @GetMapping("/{razonSocial}/necesidades")
    public ResponseEntity<List<NecesidadDTO>> obtenerNecesidades(@PathVariable String razonSocial) {
        return ResponseEntity.ok(service.obtenerNecesidades(razonSocial));
    }

    @GetMapping("/{razonSocial}/donaciones")
    public ResponseEntity<List<DonacionDTO>> obtenerDonaciones(@PathVariable String razonSocial) {
        return ResponseEntity.ok(service.obtenerDonaciones(razonSocial));
    }

    @PostMapping
    public ResponseEntity<String> registrarEntidad(@RequestBody EntidadBeneficiariaDTO dto) {
        try {
            service.registrarEntidad(dto);
            return new ResponseEntity<>("Entidad registrada con éxito", HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{razonSocial}/necesidades")
    public ResponseEntity<String> agregarNecesidad(@PathVariable String razonSocial, @RequestBody NecesidadDTO dto) {
        try {
            service.agregarNecesidad(razonSocial, dto);
            return new ResponseEntity<>("Necesidad agregada con éxito", HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}