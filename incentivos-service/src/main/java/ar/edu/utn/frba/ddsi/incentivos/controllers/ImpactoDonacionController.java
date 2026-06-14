package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.services.ImpactoDonacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@RestController
@RequestMapping("/donaciones")
public class ImpactoDonacionController {

    private final ImpactoDonacionService impactoDonacionService;

    public ImpactoDonacionController(
            ImpactoDonacionService impactoDonacionService) {
        this.impactoDonacionService = impactoDonacionService;
    }

    //lo puse como un Void, pero creo q hay q modificarlo
    @PostMapping
    public ResponseEntity<Void> recibirImpactoDonacion(@RequestBody ImpactoDonacionDTO dto) {
        impactoDonacionService.procesarImpactoDonacion(dto);
        return ResponseEntity.ok().build();
    }
}
