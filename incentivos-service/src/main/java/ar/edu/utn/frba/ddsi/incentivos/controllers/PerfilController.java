package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilPublicoDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfiles")
@Tag(name = "Gestión de Perfiles e Incentivos", description = "Endpoints para consultar métricas, misiones, insignias y rankings de los perfiles de colaboradores.")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // ========== CREAR ==========
    @Operation(
        summary = "Crear un nuevo perfil de donante",
        description = "Permite al microservicio de Donaciones solicitar la creación e inicialización de un perfil gamificado cuando un nuevo colaborador se registra en el sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Perfil creado e inicializado con éxito"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o faltantes")
    })
    @PostMapping
    public ResponseEntity<PerfilDTO> crearPerfil(@Valid @RequestBody PerfilDonanteDTO dto) {
        PerfilDTO nuevo = perfilService.crearPerfil(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(nuevo);
    }

    // ========== BUSCAR ==========
    @Operation(
        summary = "Obtener perfil por id de usuario",
        description = "Busca el perfil asociado al UUID del usuario de donaciones-service."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil obtenido con éxito"),
        @ApiResponse(responseCode = "404", description = "No existe un perfil para ese usuario")
    })
    @GetMapping("/{idUsuario}")
    public ResponseEntity<PerfilDTO> obtenerPerfilPorIdUsuario(
            @Parameter(description = "UUID del usuario asociado al perfil")
            @PathVariable UUID idUsuario) {
        return ResponseEntity.ok(perfilService.buscarPorIdUsuario(idUsuario));
    }

    @Operation(
        summary = "Obtener la misión actual asignada",
        description = "Devuelve los detalles del objetivo gamificado vigente (racha, completitud, etc.) que posee el perfil."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Misión actual obtenida con éxito"),
        @ApiResponse(responseCode = "404", description = "Perfil no encontrado o sin misión asignada")
    })
    @GetMapping("/{idUsuario}/mision")
    public ResponseEntity<MisionPerfilDTO> obtenerMisionPerfil(
            @Parameter(description = "UUID del usuario asociado al perfil")
            @PathVariable UUID idUsuario) {
        MisionPerfilDTO mision = perfilService.obtenerMisionPorIdUsuario(idUsuario);
        return ResponseEntity.ok(mision);
    }
    @Operation(
        summary = "Listar insignias obtenidas",
        description = "Retorna la colección paginada de medallas y logros desbloqueados históricamente por el colaborador."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de insignias recuperado con éxito"),
        @ApiResponse(responseCode = "404", description = "Perfil no encontrado")
    })
    @GetMapping("/{idUsuario}/insignias")
    public ResponseEntity<Page<InsigniaDTO>> obtenerInsigniasPerfil(
            @Parameter(description = "UUID del usuario asociado al perfil")
            @PathVariable UUID idUsuario,
            @ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "fechaObtencion", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(perfilService.obtenerInsigniasPorIdUsuario(idUsuario, pageable));
    }

    // ========== PÚBLICO (sin autenticación) ==========

    /**
     * Único endpoint del servicio abierto (punto 8), y vive acá con los demás de perfiles
     * en vez de en un controller aparte.
     *
     * <p>El path no colisiona con el {@code GET /{idUsuario}} de más arriba: este tiene
     * tres segmentos con un literal al final, y aquel tiene dos. La diferencia es que el de
     * arriba responde con un {@code PerfilDTO} completo y este no.
     *
     * <p>Lo que sale de acá es visible sin credenciales, así que devuelve un DTO propio y
     * acotado: nombre de usuario y nombre de categoría, nada más. Ni misión vigente, ni
     * insignias, ni identificadores internos.
     *
     * <p>El {@code permitAll()} está en {@code SecurityConfig}, como una regla por método
     * y ruta: solo el GET de ese path. No un prefijo, que abriría de más cualquier cosa
     * que se agregara después.
     */
    @Operation(
        summary = "Consultar la categoría actual de un donante (público)",
        description = "Devuelve el nombre de usuario y el nombre de su categoría actual, que es lo que el enunciado declara visible públicamente. No expone misión vigente, insignias ni identificadores internos. No requiere autenticación."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil público obtenido con éxito"),
        @ApiResponse(responseCode = "404", description = "No existe un perfil para ese usuario")
    })
    @GetMapping("/{idUsuario}/publico")
    public ResponseEntity<PerfilPublicoDTO> consultarPerfilPublico(
        @Parameter(description = "UUID del donante")
        @PathVariable UUID idUsuario) {
        return ResponseEntity.ok(perfilService.obtenerPerfilPublico(idUsuario));
    }

    // ========== ACTUALIZAR ==========
    @Operation(
        summary = "Actualizar perfil por impacto de donación",
        description = "Permite registrar una nueva donación realizada por el usuario. El servicio procesará el impacto del evento, actualizará las métricas, evaluará las reglas de las misiones y guardará el histórico."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil impactado y actualizado con éxito"),
        @ApiResponse(responseCode = "400", description = "Datos de la donación inválidos o faltantes"),
        @ApiResponse(responseCode = "404", description = "El UUID del usuario especificado no existe en los registros")
    })
    @PatchMapping("/donacion/{idUsuario}")
    public ResponseEntity<Boolean> progresarPerfil(
            @Parameter(description = "UUID del usuario que realizó la donación")
            @PathVariable UUID idUsuario,
            @Valid @RequestBody ImpactoDonacionDTO dto) {
        return ResponseEntity.ok(perfilService.actualizarPerfilImpacto(idUsuario, dto));
    }

    @Operation(
        summary = "Actualizar un perfil",
        description = "Actualiza los datos de un perfil existente."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil actualizado con éxito"),
        @ApiResponse(responseCode = "404", description = "Perfil no encontrado"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PerfilDTO> actualizarPerfil(
        @Parameter(description = "UUID del perfil a actualizar")
        @PathVariable UUID id,
        @RequestBody PerfilDTO perfil) {
        return ResponseEntity.ok(perfilService.actualizarDatosPerfil(id, perfil));
    }

    // ========== ELIMINAR ==========
    @Operation(
        summary = "Eliminar un perfil",
        description = "Elimina completamente un perfil del sistema, incluyendo sus insignias, progreso y datos asociados."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil eliminado con éxito"),
        @ApiResponse(responseCode = "404", description = "Perfil no encontrado")
    })
    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<Boolean> eliminarPerfil(
        @Parameter(description = "UUID del perfil a eliminar")
        @PathVariable UUID idUsuario) {
        Boolean eliminado = perfilService.eliminarPerfil(idUsuario);
        return ResponseEntity.ok(eliminado);
    }
}
