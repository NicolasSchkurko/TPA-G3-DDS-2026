package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
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

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // GET /personas/{uuidPersona}
    // GET permite que otros servicios obtengan los datos de PerfilDTO
    // este get debemos pensarlo para notificaciones
    @GetMapping("/{id}/perfil")
    public ResponseEntity<PerfilNotificacionDTO> obtenerPerfil(@PathVariable UUID id) {
        PerfilNotificacionDTO perfil = perfilService.buscarPerfilPorUUID(id);

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
