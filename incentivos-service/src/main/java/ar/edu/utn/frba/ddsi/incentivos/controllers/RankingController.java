package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.CrearRankingDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingResumenDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de consulta del ranking. No expone la generación: eso corre solo, una vez al mes,
 * por {@code RankingScheduler}.
 */
@RestController
@RequestMapping("/api/rankings")
@Tag(name = "Rankings",
        description = "Consulta del ranking de colaboradores y administración de los snapshots mensuales.")
public class RankingController {
    private final RankingService service;

    public RankingController(RankingService service) {
        this.service = service;
    }

    // ========== CREAR ==========
    @Operation(
            summary = "Crear ranking para un período específico",
            description = "Genera un ranking mensual según la cantidad de misiones completadas en ese período, "
                    + "sin importar la categoría. "
                    + "Se permite el mes actual como snapshot parcial, que no se recalcula automáticamente. "
                    + "Devuelve la primera página de posiciones. Es una operación de administración: "
                    + "el front valida el rol ADMIN y el backend está detrás de la ACL."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking creado con éxito"),
            @ApiResponse(responseCode = "400", description = "Período futuro o inválido"),
            @ApiResponse(responseCode = "409", description = "Ya existe un ranking para ese período"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PostMapping
    public ResponseEntity<RankingMesDTO> crearRanking(
            @Parameter(description = "UUID del administrador", required = true)
            @RequestHeader("Admin-Id") UUID idAdmin,
            @Valid @RequestBody CrearRankingDTO request) {
        RankingMesDTO rankingCreado = service.crearRankingMensual(idAdmin, request.getPeriodo());
        return ResponseEntity.ok(rankingCreado);
    }


    // ========== CONSULTAR ==========
    @Operation(
                    summary = "Consultar el puesto ranking por ID",
                    description = "Obtiene la posicion en el ranking actual para un perfil especifico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "puesto recuperado con éxito"),
            @ApiResponse(responseCode = "404", description = "perfil no encontrado")
    })
    @GetMapping("/{id}/puestoRanking")
    public ResponseEntity<RankingDTO> obtenerPuestoRankingActual(
            @Parameter(
                    description = "UUID del puesto perfil solicitado",
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.obtenerPuestoRankingActual(id));
    }

    @Operation(
            summary = "Consultar el ranking general por ID",
            description = "Obtiene una página de posiciones de un ranking específico. Por defecto devuelve 10; se pagina con page/size."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking recuperado con éxito"),
            @ApiResponse(responseCode = "404", description = "Ranking no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RankingMesDTO> obtenerRanking(
            @Parameter(description = "UUID del ranking solicitado", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @ParameterObject
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.obtenerRanking(id, pageable));
    }

    @Operation(
            summary = "Obtener el podio de colaboradores destacados",
            description = "Devuelve las primeras posiciones del ranking, ordenadas de mayor a menor puntaje. "
                    + "Por defecto devuelve el top 10; el tamaño del podio se ajusta con el parámetro `limite`. "
                    + "El ranking se persiste completo, así que el `limite` recorta la respuesta y no el "
                    + "snapshot: pedir 50 devuelve 50 aunque el ranking tenga más. El máximo es 100."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Podio recuperado con éxito"),
            @ApiResponse(responseCode = "400", description = "El límite indicado no es válido"),
            @ApiResponse(responseCode = "404", description = "Ranking no encontrado")
    })
    @GetMapping("/{id}/top")
    public ResponseEntity<RankingMesDTO> obtenerPodioRanking(
            @Parameter(description = "UUID del ranking solicitado", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Parameter(description = "Cantidad de posiciones a devolver")
            @RequestParam(name = "limite", defaultValue = "10") int limite) {
        return ResponseEntity.ok(service.obtenerRankingConLimite(id, limite));
    }

    @Operation(
            summary = "Obtener el último ranking publicado",
            description = "Retorna una página del snapshot de ranking más reciente, sin depender del mes calendario actual."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking actual recuperado con éxito"),
            @ApiResponse(responseCode = "404", description = "No existe ningún ranking publicado")
    })
    @GetMapping("/actual")
    public ResponseEntity<RankingMesDTO> obtenerRankingActual(
            @ParameterObject
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.obtenerRankingActual(pageable));
    }

    @Operation(
            summary = "Obtener historial de todos los rankings",
            description = "Retorna los encabezados paginados de los rankings generados, del más reciente al más antiguo, sin cargar sus posiciones."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial de rankings recuperado con éxito"),
            @ApiResponse(responseCode = "404", description = "No hay rankings disponibles")
    })
    @GetMapping
    public ResponseEntity<Page<RankingResumenDTO>> obtenerHistorialRankings(
            @ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "periodo", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(service.obtenerHistorialRankings(pageable));
    }

    // ========== ELIMINAR ==========
    @Operation(
            summary = "Eliminar un ranking",
            description = "Elimina un ranking específico del sistema. Es una operación de administración: el front valida el rol ADMIN y el backend está detrás de la ACL."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking eliminado con éxito"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Ranking no encontrado")
    })
    @DeleteMapping("/{idRanking}")
    public ResponseEntity<Void> eliminarRanking(
            @Parameter(description = "UUID del administrador", required = true)
            @RequestHeader("Admin-Id") UUID idAdmin,
            @Parameter(description = "UUID del ranking a eliminar", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID idRanking) {
        service.eliminarRanking(idAdmin, idRanking);
        return ResponseEntity.ok().build();
    }
}
