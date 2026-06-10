package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaObtenidaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/personas")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // GET /personas/{nombreUsuario}/perfil
    @GetMapping("/{nombreUsuario}/perfil")
    public ResponseEntity<PerfilDonanteDTO> obtenerPerfil(@PathVariable String nombreUsuario) {
        PerfilDonanteDTO perfil = perfilService.obtenerPerfil(nombreUsuario);

        if (perfil == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(perfil);
    }

    //endpoints especificos

//    GET /personas/{nombreUsuario}/metricas
    

//    GET /personas/{nombreUsuario}/misiones
//    GET /personas/{nombreUsuario}/insignias
}
