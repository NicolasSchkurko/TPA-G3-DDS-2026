package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.AltaPerfilesLoteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilPublicoDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ResultadoLotePerfilesDTO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de perfiles de donante: datos, misión en curso, insignias e historial. El perfil
 * público es el único abierto; las escrituras exigen el header {@code Admin-Id}.
 */
@RestController
@RequestMapping("/api/perfiles")
@Tag(name = "Gestión de Perfiles e Incentivos",
        description = "Endpoints para consultar métricas, misiones, insignias y rankings de los perfiles de colaboradores.")
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

    // ========== CREAR EN LOTE ==========
    @Operation(
        summary = "Crear perfiles de donante en lote",
        description = "Pensado para la importación CSV de donaciones: hasta 500 perfiles por llamada. "
                + "Un perfil que ya existe se saltea (idempotente para reintentos) y los que fallan "
                + "vienen detallados en 'errores' sin tumbar el resto."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lote procesado: ver creados/yaExistian/errores"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o faltantes")
    })
    @PostMapping("/lote")
    public ResponseEntity<ResultadoLotePerfilesDTO> crearPerfilesEnLote(
            @Valid @RequestBody AltaPerfilesLoteDTO lote) {
        return ResponseEntity.ok(perfilService.crearPerfilesEnLote(lote.getPerfiles()));
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
     * Único endpoint abierto del servicio. Devuelve un DTO acotado (nombre de usuario y
     * categoría), sin misión, insignias ni ids internos.
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
    @PutMapping("/{idUsuario}")
    public ResponseEntity<PerfilDTO> actualizarPerfil(
        @Parameter(description = "UUID del administrador", required = true)
        @RequestHeader("Admin-Id") UUID idAdmin,
        @Parameter(description = "UUID del usuario")
        @PathVariable UUID idUsuario,
        @RequestBody PerfilDTO perfil) {
        return ResponseEntity.ok(perfilService.actualizarDatosPerfil(idUsuario, idAdmin, perfil));
    }

    // ========== ELIMINAR ==========
    @Operation(
        summary = "Eliminar un perfil",
        description = "Elimina completamente un perfil del sistema, incluyendo sus insignias, progreso y datos asociados."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil eliminado con éxito"),
        @ApiResponse(responseCode = "403", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Perfil no encontrado")
    })
    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<Void> eliminarPerfil(
        @Parameter(description = "UUID del administrador", required = true)
        @RequestHeader("Admin-Id") UUID idAdmin,
        @Parameter(description = "UUID del perfil a eliminar")
        @PathVariable UUID idUsuario) {
        perfilService.eliminarPerfil(idUsuario, idAdmin);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Eliminar perfil por baja de donante (uso interno)",
        description = "Lo invoca donaciones-service cuando se elimina un donante, para que el "
            + "perfil gamificado (y su historial de impacto de donaciones) no quede húerfano. "
            + "No pide Admin-Id: la baja ya la autorizó donaciones-service, esto es la "
            + "consecuencia automática, no una acción de administrador nueva. Idempotente: si el "
            + "donante nunca llegó a tener perfil, no hace nada y responde 200 igual."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "Perfil (y su historial de donaciones) eliminado, o no existía")
    })
    @DeleteMapping("/interno/{idUsuario}")
    public ResponseEntity<Void> eliminarPerfilPorBajaDeDonante(
        @Parameter(description = "UUID del donante dado de baja")
        @PathVariable UUID idUsuario) {
        perfilService.eliminarPerfilPorBajaDeDonante(idUsuario);
        return ResponseEntity.ok().build();
    }
}
