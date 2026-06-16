package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/perfiles")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

//permite q Donaciones nos pida crear un Perfil
    @PostMapping
    public ResponseEntity<Void> crearPerfil(@RequestBody PerfilDonanteDTO dto) {
        perfilService.crearPerfil(dto);
        return ResponseEntity.ok().build();
    }

//permite q Donaciones nos pida actualizarPerfil ante una donacion
    @PostMapping("/{id}/perfil")
    public ResponseEntity<Void> actualizarPerfil(@RequestBody ImpactoDonacionDTO dto) {
        //Recibe donacion, actualizar perfil y guardan en repo de donaciones
        perfilService.actualizarPerfil(dto);
        return ResponseEntity.ok().build();
    }
}