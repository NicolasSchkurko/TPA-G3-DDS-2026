package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.services.LogisticaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping("/entrega")
public class EntregasController {
    private final LogisticaService logisticaService;

    public EntregasController(LogisticaService logisticaService) {
        this.logisticaService = logisticaService;
    }

    @PostMapping("/{id}/entregada")
    public ResponseEntity<String> entregarPaquete(@PathVariable UUID id) {
        logisticaService.entregarPaquete(id);
        return ResponseEntity.ok("Paquete: " + id + " entregado correctamente");
    }

    @PostMapping("/{id}/no_recibida")
    public ResponseEntity<String> paqueteNoRecibido(@PathVariable UUID id) {
        logisticaService.paqueteNoRecibido(id);
        return ResponseEntity.ok("El paquete " + id + " no fue recibido");
    }
}
