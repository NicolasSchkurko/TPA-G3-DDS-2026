package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import ar.edu.utn.frba.ddsi.incentivos.dto.ActividadMensualDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MetricasActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/perfiles")
public class PerfilController {
    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @GetMapping("/{id}/comparativa")
    public ResponseEntity<MetricasActividadDTO> obtenerComparativaPerfil(@PathVariable UUID id) {
        MetricasActividad metrica = service.obtenerMetricasDonante(id);
        if (metrica == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.convertirMetricaADTO(metrica));
    }

    @GetMapping("/{id}/metricas")
    public ResponseEntity<List<ActividadMensualDTO>> obtenerMetricasPerfil(@PathVariable UUID id){
        List<ActividadMensual> metricas = service.obtenerEvolucionHistorica(id);
        if (metricas == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metricas.stream().map(service::convertirActividadADTO).toList());
    }

    @GetMapping("/{id}/mision")
    public ResponseEntity<MisionDTO> obtenerMisionPerfil(@PathVariable UUID id) {
        Mision mision = service.obtenerMisionPorID(id);
        if (mision == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(service.convertirMisionADTO(mision));
    }

    @GetMapping("/{id}/insignias")
    public ResponseEntity<List<InsigniaDTO>> obtenerInsigniasPerfil(@PathVariable UUID id) {
        List<Insignia> insignias = service.obtenerInsigniasPorID(id);
        if (insignias == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(insignias.stream().map(service::convertirInsigniaADTO).toList());
    }

    //Tal vez en el futuro sea necesario los endpoints para los metodos de ranking (generar ranking y dar el top 3)
}