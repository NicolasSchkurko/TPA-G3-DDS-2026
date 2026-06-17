package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
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

    //le habilito al perfil ver el ranking del mes y top3, no se si sea necesario el id en la ruta
    @GetMapping("/{id}/ranking")
    public ResponseEntity<List<RankingDTO>> obtenerRankingActual(@PathVariable UUID id) {
        RankingMensual rankingMes = service.obtenerRankingDelMes(YearMonth.from(LocalDate.now().getMonth()));
        if (rankingMes == null) {
            return ResponseEntity.notFound().build();
        }

        List<RankingDTO> dto = rankingMes.getPosiciones().stream()
                .map(service::convertirRankingADTO)
                .toList();

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/ranking/top3")
    public ResponseEntity<List<RankingDTO>> obtenerTop3RankingActual(@PathVariable UUID id) {
        List<Ranking> top3 = service.obtenerTop3DelMes(YearMonth.from(LocalDate.now().getMonth()));
        if (top3 == null) {
            return ResponseEntity.notFound().build();
        }

        List<RankingDTO> dto = top3.stream()
                .map(service::convertirRankingADTO)
                .toList();

        return ResponseEntity.ok(dto);
    }
}