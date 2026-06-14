package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import ar.edu.utn.frba.ddsi.incentivos.dto.MetricasActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaDTO;
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

    @GetMapping("/perfil")
    public ResponseEntity<MisionDTO> obtenerMisionPerfil() {

        MisionDTO mision = new Mision();
//logica
        return ResponseEntity.ok(mision);
    }

    @GetMapping("/perfil")
    public ResponseEntity<List<InsigniaDTO>> obtenerInsigniasPerfil() {
        
        List<Insignia> insignias = perfilService.listarInsignias();

        List<InsigniaDTO> dtoList = insignias.stream()
            .map(insignia -> new InsigniaDTO(
//campos de InsigniaDTO
                ))
            .toList(); 

        return ResponseEntity.ok(dtoList);
    }
}
