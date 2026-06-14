package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import ar.edu.utn.frba.ddsi.incentivos.dto.MetricasActividadDTO
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perfiles") // Coincide con la ruta del Gateway
public class PerfilController {
//para mostrarle al cliente persona
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/perfil")
    public ResponseEntity<MetricasActividadDTO> obtenerMetricasPerfil() {

        MetricasActividadDTO metricas = new MetricasActividadDTO();
//logica
        return ResponseEntity.ok(metricas);
    }
}
