package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.DonacionDestacadaDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.DestacadasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Landing público: últimas donaciones entregadas que completaron una misión. */
@RestController
@RequestMapping("/api/donaciones")
@Tag(name = "Donaciones", description = "Donaciones destacadas para el landing público.")
public class DonacionDestacadaController {

    private final DestacadasService service;

    public DonacionDestacadaController(DestacadasService service) {
        this.service = service;
    }

    /**
     * Devuelve las donaciones destacadas, sin datos personales.
     *
     * @param limite cantidad de donaciones; si se omite se usa la property configurada
     * @return lista de donaciones sin datos personales, con caché de 5 minutos
     */
    @Operation(summary = "Obtener donaciones destacadas",
            description = "Últimas donaciones entregadas que completaron una misión.")
    @ApiResponse(responseCode = "200", description = "Lista de donaciones destacadas")
    @GetMapping("/destacadas")
    public ResponseEntity<List<DonacionDestacadaDTO>> destacadas(
            @Parameter(description = "Cantidad de donaciones, entre 1 y 100")
            @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(service.obtenerDestacadas(limite));
    }
}
