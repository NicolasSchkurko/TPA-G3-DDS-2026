package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.services.PersonaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/perfiles")
public class PersonaController {
    private final PersonaService personaService;

    public PersonaController(PersonaService perfilService) {
        this.personaService = perfilService;
    }

    //https:localhost/perfiles
//permite q Donaciones nos pida crear un Perfil
    @PostMapping
    public ResponseEntity<PerfilDTO> crearPerfil(@RequestBody PerfilDonanteDTO dto) {
        try {
            PerfilDTO nuevo = personaService.crearPerfil(dto);
            return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    //https:localhost/perfiles/{idUsuario}
//permite q Donaciones nos pida actualizarPerfil ante una donacion
    @PutMapping("/{id}")
    public ResponseEntity<PerfilDTO> actualizarPerfil(@PathVariable UUID id,
                                                      @RequestBody ImpactoDonacionDTO dto) {
        //Recibe donacion, actualizar perfil y guardan en repo de donaciones
        try {
            PerfilDTO actualizado = personaService.actualizarPerfil(id, dto);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}