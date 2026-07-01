package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.DestinoEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.PeticionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.LogisticaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logistica") //https:localhost:8001/logistica
public class LogisticaCtrl {
    private final LogisticaService logisticaService;

    public LogisticaCtrl(LogisticaService logisticaService) {
        this.logisticaService = logisticaService;
    }

    // CREATE (C) - Endpoint que entrega a donaciones-serv la lista de destinos
//    @PostMapping
//    public ResponseEntity<List<DestinoEntregaDTO>> crearDestinos(@RequestBody PeticionEntregaDTO request) {
//        List<DestinoEntregaDTO> destinos =
//                logisticaService.procesarPeticion(request);
//        if (destinos == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        return ResponseEntity.ok(destinos);
//    }

    //habilitar endpoint para pasar a donaciones InfoDestinosDTO a donaciones
}


