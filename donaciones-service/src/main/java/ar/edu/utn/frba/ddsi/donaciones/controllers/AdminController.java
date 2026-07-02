package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.RevisionEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.services.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admins")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/entregas/no-recibidas")
    public ResponseEntity<List<RutaEnProceso>> obtenerEntregasNoRecibidas() {
        return ResponseEntity.ok(service.obtenerEntregasNoRecibidas());
    }

    @PatchMapping("/entregas/no-recibidas/{idEntrega}")
    public ResponseEntity<Void> revisarEntregaNoRecibida(
            @PathVariable UUID idEntrega,
            @RequestBody RevisionEntregaDTO dto
    ) {
        try {
            service.revisarEntregaNoRecibida(idEntrega, dto.getEstadoEntrega());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("No se encontro")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }
}
