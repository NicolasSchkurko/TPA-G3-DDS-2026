package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/perfiles") //https:localhost:8001/api/perfiles
@Tag(name = "Gestión de Perfiles e Incentivos", description = "Endpoints para consultar métricas, misiones, insignias y rankings de los perfiles de colaboradores.")
public class PerfilController {
    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> eliminarPerfil(@PathVariable UUID id) {
//        service.eliminarPerfil(id);
//        return ResponseEntity.noContent().build();
//    }

    //https:localhost:8001/perfiles/{id}/comparativa
    @Operation(
            summary = "Obtener métricas comparativas del donante",
            description = "Devuelve los indicadores clave de rendimiento del perfil comparados con promedios generales."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas obtenidas con éxito"),
            @ApiResponse(responseCode = "404", description = "Perfil no encontrado o sin actividad")
    })
    @GetMapping("/{id}/comparativas/{idPerfil}")
    public ResponseEntity<MetricasHistoricasDTO> obtenerMetricas(@Parameter(description = "UUID del usuario asociado al perfil", example = "123e4567-e89b-12d3-a456-426614174000")
                                                                      @PathVariable UUID idUsuario, @PathVariable UUID idPerfil) {
        MetricasHistoricasDTO metricas = service.obtenerMetricasDonante(idUsuario, idPerfil);
        if (metricas == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metricas);
    }

    @Operation(
            summary = "Obtener perfil por id de usuario",
            description = "Busca el perfil asociado al UUID del usuario de donaciones-service."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil obtenido con éxito"),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para ese usuario")
    })
    @GetMapping("{idUsuario}/perfil")
    public ResponseEntity<PerfilDTO> obtenerPerfilPorIdUsuario(
            @Parameter(description = "UUID del usuario asociado al perfil")
            @PathVariable UUID idUsuario) {
        PerfilDTO perfil = service.buscarPorIdUsuario(idUsuario);
        return perfil == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(perfil);
    }

    @Operation(
            summary = "Obtener evolución de actividad mensual",
            description = "Retorna el historial mes a mes de las donaciones y colaboraciones realizadas por el usuario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial mensual obtenido con éxito"),
            @ApiResponse(responseCode = "404", description = "Perfil no encontrado")
    })
    @GetMapping("/{id}/actividadPerfil/{idPerfil}")
    public ResponseEntity<ActividadDTO> obtenerActividadPerfil(@Parameter(description = "UUID del usuario asociado al perfil", example = "123e4567-e89b-12d3-a456-426614174000")
                                                                            @PathVariable UUID idUsuario, @PathVariable UUID idPerfil){
        ActividadDTO dto = service.obtenerEvolucionHistorica(idUsuario, idPerfil);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Obtener la misión actual asignada",
            description = "Devuelve los detalles del objetivo gamificado vigente (racha, completitud, etc.) que posee el perfil."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Misión actual obtenida con éxito"),
            @ApiResponse(responseCode = "404", description = "Perfil no encontrado o sin misión asignada")
    })
    @GetMapping("/{id}/mision")
    public ResponseEntity<MisionPerfilDTO> obtenerMisionPerfil(@Parameter(description = "UUID del usuario asociado al perfil", example = "123e4567-e89b-12d3-a456-426614174000")
                                                         @PathVariable UUID id) {
        MisionPerfilDTO mision = service.obtenerMisionPorID(id);
        if (mision == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mision);
    }

    @Operation(
            summary = "Listar insignias obtenidas",
            description = "Retorna la colección de medallas y logros desbloqueados históricamente por el colaborador."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de insignias recuperado con éxito"),
            @ApiResponse(responseCode = "404", description = "Perfil no encontrado")
    })
    @GetMapping("/{id}/insignias")
    public ResponseEntity<ListaInsigniasDTO> obtenerInsigniasPerfil(@Parameter(description = "UUID del usuario asociado al perfil", example = "123e4567-e89b-12d3-a456-426614174000")
                                                                    @PathVariable UUID id) {
        ListaInsigniasDTO insignias = service.obtenerInsigniasPorID(id);
        if (insignias == null) {
            return ResponseEntity.notFound().build();
        }


        return ResponseEntity.ok(insignias);
    }

    //le habilito al perfil ver el ranking del mes y top3, no se si sea necesario el id en la ruta
    @Operation(
            summary = "Consultar el ranking general del mes corriente",
            description = "Obtiene la lista de puntajes y posiciones de todos los colaboradores para el mes en curso."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking del mes recuperado con éxito"),
            @ApiResponse(responseCode = "404", description = "No se encontró un ranking generado para el periodo actual")
    })
    @GetMapping("/ranking/{id}")
    public ResponseEntity<RankingMesDTO> obtenerRanking(@Parameter(description = "UUID del ranking solicitado", example = "123e4567-e89b-12d3-a456-426614174000")
                                                                  @PathVariable UUID id) {
        RankingMesDTO rankingMes = service.obtenerRanking(id);
        if (rankingMes == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rankingMes);
    }

    @Operation(
            summary = "Obtener el Top 3 de colaboradores destacados",
            description = "Endpoint optimizado para tableros que lista exclusivamente a los tres usuarios con mayor puntuación acumulada en el mes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Podio recuperado con éxito"),
            @ApiResponse(responseCode = "404", description = "Datos del podio no disponibles")
    })
    @GetMapping("/ranking/{id}/top3")
    public ResponseEntity<RankingMesDTO> obtenerTop3Ranking(@Parameter(description = "UUID del ranking solicitado", example = "123e4567-e89b-12d3-a456-426614174000")
                                                                      @PathVariable UUID id) {
        RankingMesDTO top3 = service.obtenerTop3Ranking(id);
        if (top3 == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(top3);
    }
}
