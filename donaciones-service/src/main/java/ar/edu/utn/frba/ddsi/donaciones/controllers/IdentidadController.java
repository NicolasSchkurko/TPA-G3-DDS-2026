package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.identidad.IdentidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.IdentidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Identidad", description = "Resuelve los perfiles vinculados a un correo electrónico")
@RequestMapping("/api/identidades")
public class IdentidadController {

    private final IdentidadService service;

    public IdentidadController(IdentidadService service) {
        this.service = service;
    }

    @Operation(summary = "Perfiles vinculados a un email")
    @GetMapping("/por-email")
    public ResponseEntity<?> porEmail(@RequestParam String email) {
        try {
            return ResponseEntity.ok(service.porEmail(email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
