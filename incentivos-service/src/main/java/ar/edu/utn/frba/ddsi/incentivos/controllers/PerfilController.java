package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/personas")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    //endpoints especificos

//    POST /personas/{idDonacion}
//    @PostMapping("/{idDonacion}")
//    public ResponseEntity<Void>
//    procesarDonacion(
//            @PathVariable UUID idDonacion) {
//
//        perfilService
//                .procesarDonacion(
//                        idDonacion);
//
//        return ResponseEntity.ok()
//                .build();
//    }

//    GET /personas/{uuid}/metricas
//    GET /personas/{uuid}/misiones
//    GET /personas/{uuid}/insignias
}
