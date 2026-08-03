package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.SecuenciaCategoriasDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin") //https:localhost:8001/api/admin
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping("/categorias")
    public ResponseEntity<SecuenciaCategoriasDTO> crearCategoria(@RequestBody CategoriaDTO request) {
        SecuenciaCategoriasDTO nuevaSecuencia = service.agregarCategoria(request);
        if (nuevaSecuencia == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(nuevaSecuencia);
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<SecuenciaCategoriasDTO> eliminarCategoria(@PathVariable UUID id) {
        SecuenciaCategoriasDTO nuevaSecuencia = service.eliminarCategoria(id);
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

//    @PostMapping("/misiones")
//    public ResponseEntity<MisionDTO> crearMision(@RequestBody MisionDTO request) {
//        Mision nuevaMision = service.agregarMision(request);
//        if (nuevaMision == null) {
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(service.misionToDTO(nuevaMision));
//    }

}