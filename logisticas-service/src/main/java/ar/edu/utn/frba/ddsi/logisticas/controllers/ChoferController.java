package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.services.RutaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/choferes")
public class ChoferController {

    private final RutaService rutaService;

    public ChoferController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @PostMapping("/{id}/iniciar_ruta")
    public ResponseEntity<String> iniciarRuta(@PathVariable UUID id) {
        rutaService.iniciarRuta(id);
        return ResponseEntity.ok("Ruta inicializada para el chofer " + id);
    }

    @PostMapping("/{id}/terminar_ruta")
    public ResponseEntity<String> terminarRuta(@PathVariable UUID id){
        rutaService.terminarRuta(id);
        return ResponseEntity.ok("Ruta finalizada para el chofer " + id);
    }
}