package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import ar.edu.utn.frba.ddsi.incentivos.dto.MetricasActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perfiles")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

//permite q Donaciones nos pida crear un Perfil
//TODO CrearPerfilDTO y crearPerfil(dto)
//CrearPerfilDTO tiene la informacion necesaria del servicio de donaciones para crear el perfil
    @PostMapping
    public ResponseEntity<Void> crearPerfil(@RequestBody CrearPerfilDTO dto) {
        perfilService.crearPerfil(dto);
        return ResponseEntity.ok().build();
    }
}

//permite q Donaciones nos pida actualizarPerfil ante una donacion
//TODO ActualizarPerfilDTO y actualizarPerfil(dto)
//ActualizarPerfilDTO tiene la informacion necesaria del servicio de donaciones para actualizar el perfil (debe ser una donacion nueva)
    @PostMapping
    public ResponseEntity<Void> actualizarPerfil(@RequestBody ActualizarPerfilDTO dto) {
        perfilService.actualizarPerfil(dto);
        return ResponseEntity.ok().build();
    }
}

//esto va al apiGateway?
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
