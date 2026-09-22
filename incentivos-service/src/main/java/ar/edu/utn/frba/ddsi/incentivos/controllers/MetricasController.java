package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MetricaDonacionesDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.MetricasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Métricas", description = "Endpoints para obtener métricas y análisis de donaciones y actividad de perfiles.")
public class MetricasController {
    private final MetricasService service;

    public MetricasController(MetricasService service) {
        this.service = service;
    }

    // ========== MÉTRICAS POR PERÍODO ==========
    @Operation(
        summary = "Obtener métricas de donaciones por período",
        description = "Devuelve datos agregados (cantidad, total, categorías) para que el frontend realice comparaciones entre períodos."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Métrica obtenida con éxito"),
        @ApiResponse(responseCode = "404", description = "No hay donaciones en el período especificado"),
        @ApiResponse(responseCode = "400", description = "Parámetros de fecha inválidos")
    })
    @GetMapping("/{idUsuario}/periodo")
    public ResponseEntity<MetricaDonacionesDTO> obtenerMetricaPorPeriodo(
            @Parameter(description = "UUID del usuario")
            @PathVariable UUID idUsuario,
            @Parameter(description = "Fecha inicial del período (formato: yyyy-MM-dd)")
            @RequestParam LocalDate desde,
            @Parameter(description = "Fecha final del período (formato: yyyy-MM-dd)")
            @RequestParam LocalDate hasta
    ) {
        return service.obtenerMetrica(idUsuario, desde, hasta)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ========== ACTIVIDAD HISTÓRICA ==========
    @Operation(
        summary = "Obtener evolución histórica de actividad del perfil",
        description = "Retorna un registro completo de la evolución y actividad del usuario en el sistema, incluyendo donaciones, misiones completadas y cambios de categoría."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Actividad recuperada con éxito"),
        @ApiResponse(responseCode = "404", description = "Perfil o actividad no encontrada")
    })
    @GetMapping("/{idUsuario}/actividad")
    public ResponseEntity<ActividadDTO> obtenerActividadPerfil(
            @Parameter(description = "UUID del usuario")
            @PathVariable UUID idUsuario
    ) {
        ActividadDTO actividad = service.obtenerEvolucionHistorica(idUsuario);
        return actividad == null
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(actividad);
    }
}
