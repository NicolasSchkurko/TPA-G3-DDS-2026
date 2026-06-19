package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.services.PersonaService;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<PerfilDTO> crearPerfil(@RequestBody PerfilDonanteDTO dto) {
        PerfilDTO nuevo = personaService.crearPerfil(dto);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }

    //https:localhost/perfiles/{idUsuario}/perfil
//permite q Donaciones nos pida actualizarPerfil ante una donacion
    @PostMapping("/{id}/perfil")
    public ResponseEntity<PerfilDTO> actualizarPerfil(@RequestBody ImpactoDonacionDTO dto) {
        //Recibe donacion, actualizar perfil y guardan en repo de donaciones
        PerfilDTO actualizado = personaService.actualizarPerfil(dto);
        return new ResponseEntity<>(actualizado, HttpStatus.CREATED);
    }
}