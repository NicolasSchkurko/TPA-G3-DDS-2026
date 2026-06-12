package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.ImpactoDonacionService;
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

    @GetMapping("/{idDonacion}")
    public ResponseEntity<ImpactoDonacionDTO> obtenerDonacion(
            @PathVariable UUID idDonacion) {

        ImpactoDonacionDTO dto =
                impactoDonacionService.buscarDonacionPorUUID(
                        idDonacion);

        if(dto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dto);
    }
}
