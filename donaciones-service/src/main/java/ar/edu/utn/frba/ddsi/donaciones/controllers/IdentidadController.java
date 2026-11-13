package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.identidad.IdentidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.identidad.VincularIdentidadRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.IdentidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Identidad", description = "Resuelve los perfiles de dominio vinculados a una identidad")
@RequestMapping("/api/identidades")
public class IdentidadController {

    private final IdentidadService service;

    public IdentidadController(IdentidadService service) {
        this.service = service;
    }

    @Operation(summary = "Perfiles vinculados a un email (fallback transitorio)")
    @Deprecated(since = "2026-11", forRemoval = true)
    @GetMapping("/por-email")
    public ResponseEntity<?> porEmail(@RequestParam String email) {
        try {
            return ResponseEntity.ok(service.porEmail(email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Perfiles vinculados a una cuenta de Keycloak (claim sub)")
    @GetMapping("/por-sub/{sub}")
    public ResponseEntity<?> porSub(@PathVariable String sub) {
        try {
            return ResponseEntity.ok(service.porSub(sub));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Vincula una cuenta de Keycloak con un perfil de dominio")
    @PostMapping("/vincular")
    public ResponseEntity<?> vincular(@RequestBody VincularIdentidadRequest request) {
        try {
            service.vincular(request.getSub(), request.getRol(), request.getPerfilId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
