package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import ar.edu.utn.frba.ddsi.incentivos.services.DonacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/personas")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // GET /personas/{uuid}/perfil
    @GetMapping("/{id}/perfil")
    public ResponseEntity<PerfilDonanteDTO> obtenerPerfil(@PathVariable UUID id) {
        PerfilDonanteDTO perfil = perfilService.buscarPerfilPorUUID(id);

        if (perfil == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(perfil);
    }

    //endpoints especificos

    //no necesito todas las donaciones, solo la ultima
//    GET /personas/{uuid}/donaciones
    @GetMapping("/{id}/donaciones")
    public ResponseEntity<DonacionDTO> obtenerDonacion(@PathVariable UUID id) {
        DonacionDTO donacion = donacionService.obtenerDonacion(id);

        if (donacion == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(donacion);
    }

//    GET /personas/{uuid}/metricas
//    GET /personas/{uuid}/misiones
//    GET /personas/{uuid}/insignias
}
