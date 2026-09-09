package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MetricaDonacionesDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.MetricasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/metricas")
public class MetricasController {
    private final MetricasService service;

    public MetricasController(MetricasService service) {
        this.service = service;
    }

    @Operation(
            summary = "Obtener métricas de donaciones por período",
            description = "Devuelve datos agregados para que el frontend realice comparaciones."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métrica obtenida con éxito"),
            @ApiResponse(responseCode = "404", description = "No hay donaciones en el período")
    })
    @GetMapping("/{idUsuario}")
    public ResponseEntity<MetricaDonacionesDTO> obtenerMetrica(
            @Parameter(description = "UUID del usuario")
            @PathVariable UUID idUsuario,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta
    ) {
        return service.obtenerMetrica(idUsuario, desde, hasta)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{idUsuario}/actividadPerfil")
    public ResponseEntity<ActividadDTO> obtenerActividadPerfil(
            @PathVariable UUID idUsuario) {
        return ResponseEntity.ok(service.obtenerEvolucionHistorica(idUsuario));
    }
}
