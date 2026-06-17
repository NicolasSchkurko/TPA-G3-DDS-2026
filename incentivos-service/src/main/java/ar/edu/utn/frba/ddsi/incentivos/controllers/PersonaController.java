package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.services.PersonaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Void> crearPerfil(@RequestBody PerfilDonanteDTO dto) {
        personaService.crearPerfil(dto);
        return ResponseEntity.ok().build();
    }

    //https:localhost/perfiles/{idUsuario}/perfil
//permite q Donaciones nos pida actualizarPerfil ante una donacion
    @PostMapping("/{id}/perfil")
    public ResponseEntity<Void> actualizarPerfil(@RequestBody ImpactoDonacionDTO dto) {
        //Recibe donacion, actualizar perfil y guardan en repo de donaciones
        personaService.actualizarPerfil(dto);
        return ResponseEntity.ok().build();
    }
}