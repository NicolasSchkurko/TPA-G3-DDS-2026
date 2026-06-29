package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin") //https:localhost:8001/admin
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping("/categorias")
    public ResponseEntity<SecuenciaDTO> crearCategoria(@RequestBody CategoriaDTO request) {
        SecuenciaCategorias nuevaSecuencia = service.agregarCategoria(request);
        if (nuevaSecuencia == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.secuenciaToDTO(nuevaSecuencia));
    }

    @PostMapping("/misiones")
    public ResponseEntity<MisionDTO> crearMision(@RequestBody MisionDTO request) {
        Mision nuevaMision = service.agregarMision(request);
        if (nuevaMision == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.misionToDTO(nuevaMision));
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<DonacionDTO> actualizarCategoria(@PathVariable UUID id, @RequestBody CategoriaDTO categoria) {
        try {
            Donacion actualizada = donacionService.actualizarDonacion(id, donacion);
            return ResponseEntity.ok(donacionService.toDTO(actualizada));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
