package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.DestinoEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.DestinosDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.PeticionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.services.LogisticaService;
import org.springframework.http.HttpStatus;
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

    //  CREATE (C) - Endpoint que entrega a donaciones-serv la lista de destinos
    @PostMapping
    public ResponseEntity<DestinosDTO> crearDestinos(@RequestBody PeticionEntregaDTO request) {
        DestinosDTO destinos = new DestinosDTO();
        destinos.setDestinos(logisticaService.procesarPeticion(request));
        return ResponseEntity.ok(destinos);
    }

    // CREATE (C) - Endpoint que hace que logistica reciba los camiones disponibles
    @PostMapping
    public ResponseEntity<List<CamionDTO>> registrarCamiones(@RequestBody List<CamionDTO> request){
        try {
            List<Camion> camiones = request.stream()
                    .map(logisticaService::convertirDTO)
                    .toList();
            logisticaService.guardarCamiones(camiones);
            List<CamionDTO> response = camiones.stream()
                    .map(logisticaService::convertirACamionDTO)
                    .toList();

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }


    //habilitar endpoint para pasar a donaciones InfoDestinosDTO a donaciones
}