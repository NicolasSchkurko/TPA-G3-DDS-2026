package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.services.LogisticaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/choferes")
public class ChoferController {

    private final LogisticaService logisticaService;

    public ChoferController(LogisticaService logisticaService) {
        this.logisticaService = logisticaService;
    }

    @PostMapping("/{id}/iniciar_ruta")
    public ResponseEntity<String> iniciarRuta(@PathVariable UUID id) {
        logisticaService.iniciarRuta(id);
        return ResponseEntity.ok("Ruta Inicializada del chofer " + id);
    }

    @PostMapping("/{id}/terminar_ruta")
    public ResponseEntity<String> terminarRuta(@PathVariable UUID id){
        logisticaService.terminarRuta(id);
        return ResponseEntity.ok("Ruta finalizada del chofer " + id);
    }
}