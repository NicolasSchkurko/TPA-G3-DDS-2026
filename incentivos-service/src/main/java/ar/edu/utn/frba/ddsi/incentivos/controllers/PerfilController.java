package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import ar.edu.utn.frba.ddsi.incentivos.services.ImpactoDonacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@RestController
@RequestMapping("/personas")
public class PerfilController {

    private final PerfilService perfilService;
    private final RestTemplate restTemplate;
    private final ImpactoDonacionService impactoDonacionService;

    public PerfilController(PerfilService perfilService,
                            ImpactoDonacionService impactoDonacionService,
                            RestTemplate restTemplate) {
        this.perfilService = perfilService;
        this.impactoDonacionService = impactoDonacionService;
        this.restTemplate = restTemplate;
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

    // tomando inspiracion de notificaciones-service xd
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
