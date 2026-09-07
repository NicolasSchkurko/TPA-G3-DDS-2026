package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin") //https:localhost:8001/api/admin
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> crearCategoria(
        @RequestHeader("Admin-Id") UUID idAdmin,
        @RequestBody CategoriaDTO request) {
        List<CategoriaDTO> nuevaSecuencia = service.agregarCategoria(idAdmin, request);
        return ResponseEntity.ok(nuevaSecuencia);
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<List<CategoriaDTO>> eliminarCategoria(
        @RequestHeader("Admin-Id") UUID idAdmin,
        @PathVariable UUID id) {
        List<CategoriaDTO> nuevaSecuencia = service.eliminarCategoria(idAdmin, id);
        return ResponseEntity.ok(nuevaSecuencia);
    }

    @PutMapping("/categorias/modificar/{id}")
    public ResponseEntity<CategoriaDTO> actualizarCategoria(
        @RequestHeader("Admin-Id") UUID idAdmin,
        @PathVariable UUID id,
        @RequestBody CategoriaDTO categoria) {
        CategoriaDTO actualizada = service.actualizarCategoria(idAdmin, id, categoria);
        return actualizada == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(actualizada);
    }

    @PostMapping("/misiones")
    public ResponseEntity<MisionDTO> crearMision(
        @RequestHeader("Admin-Id") UUID idAdmin,
        @RequestBody MisionDTO request) {
        MisionDTO nuevaMision = service.crearMision(idAdmin, request);
        return ResponseEntity.ok(nuevaMision);
    }

    @DeleteMapping("/misiones/{id}")
    public ResponseEntity<MisionDTO> eliminarMision(
        @RequestHeader("Admin-Id") UUID idAdmin,
        @PathVariable UUID id) {
        MisionDTO misionEliminada = service.eliminarMision(idAdmin, id);
        return misionEliminada == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(misionEliminada);
    }

    @PutMapping("/misiones/modificar/{id}")
    public ResponseEntity<MisionDTO> actualizarMision(
        @RequestHeader("Admin-Id") UUID idAdmin,
        @PathVariable UUID id,
        @RequestBody MisionDTO mision) {
        MisionDTO actualizada = service.actualizarMision(idAdmin, id, mision);
        return actualizada == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(actualizada);
    }
}