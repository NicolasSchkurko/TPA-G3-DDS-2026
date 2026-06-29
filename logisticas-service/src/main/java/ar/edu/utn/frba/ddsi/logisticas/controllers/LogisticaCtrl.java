package ar.edu.utn.frba.ddsi.logisticas.controllers;

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

    // CREATE (C) - Endpoint que procesa el formulario completo y segmentar
    @PostMapping("/formulario")
    public ResponseEntity<List<DonacionDTO>> crearDonacion(@RequestBody FormularioRequestDTO request) {
        List<Donacion> donacionesSegmentadas = donacionService.procesarFormulario(
                request.getDonante(),
                request.getBienes(),
                request.getFechaRealizacion()
        );
        if (donacionesSegmentadas == null) {
            return ResponseEntity.notFound().build();
        }

        List<DonacionDTO> dto = donacionesSegmentadas.stream()
                .map(donacionService::toDTO)
                .toList();

        return ResponseEntity.ok(dto);
    }
}
