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
    public ResponseEntity<List<CategoriaDTO>> crearCategoria(@RequestBody CategoriaDTO request) {
        List<CategoriaDTO> nuevaSecuencia = service.agregarCategoria(request);
        if (nuevaSecuencia == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(nuevaSecuencia);
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<List<CategoriaDTO>> eliminarCategoria(@PathVariable UUID id) {
        List<CategoriaDTO> nuevaSecuencia = service.eliminarCategoria(id);
        if (nuevaSecuencia == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(nuevaSecuencia);
    }

    @PutMapping("/categorias/modificar/{id}")
    public ResponseEntity<CategoriaDTO> actualizarCategoria(@PathVariable UUID id, @RequestBody CategoriaDTO categoria) {
        try {
            CategoriaDTO actualizada = service.actualizarCategoria(id, categoria);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/misiones")
    public ResponseEntity<MisionDTO> crearMision(@RequestBody MisionDTO request) {
        MisionDTO nuevaMision = service.crearMision(request);
        if (nuevaMision == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(nuevaMision);
    }

    @DeleteMapping("/misiones/{id}")
    public ResponseEntity<MisionDTO> eliminarMision(@PathVariable UUID id) {
        MisionDTO misionEliminada = service.eliminarMision(id);
        if (misionEliminada == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(misionEliminada);
    }

    @PutMapping("/misiones/modificar/{id}")
    public ResponseEntity<MisionDTO> actualizarMision(@PathVariable UUID id, @RequestBody MisionDTO mision) {
        try {
            MisionDTO actualizada = service.actualizarMision(id, mision);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
