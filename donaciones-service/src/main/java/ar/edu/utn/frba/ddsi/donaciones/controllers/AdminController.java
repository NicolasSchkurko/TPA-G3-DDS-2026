package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "Crear/registrar nuevo admin")
    @PostMapping
    public ResponseEntity<?> registrarAdmin(@RequestBody AdminDTO dto) {
        try {
            AdminDTO adminCreado = service.crearAdministrador(dto);
            return new ResponseEntity<>(adminCreado, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Obtener todos los admin")
    @GetMapping
    public ResponseEntity<List<AdminDTO>> obtenerTodos() {
        return ResponseEntity.ok(service.getAdmins());
    }

    @Operation(summary = "Obtener admin por id")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.getAdminPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Actualizar admin por id")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarAdmin(@PathVariable UUID id, @RequestBody AdminDTO dto) {
        try {
            AdminDTO actualizada = service.actualizarAdmin(id, dto);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar admin por id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAdmin(@PathVariable UUID id) {
        service.eliminarAdmin(id);
        return ResponseEntity.noContent().build();
    }
}