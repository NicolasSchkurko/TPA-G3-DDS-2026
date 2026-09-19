package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/perfiles")
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
    public ResponseEntity<PerfilDTO> crearPerfil(@RequestBody PerfilDonanteDTO dto) {
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
        PerfilDTO perfil = perfilService.buscarPorIdUsuario(idUsuario);
        return perfil == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(perfil);
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

    // TODO: PAGINACION

    @Operation(
        summary = "Listar insignias obtenidas",
        description = "Retorna la colección de medallas y logros desbloqueados históricamente por el colaborador."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de insignias recuperado con éxito"),
        @ApiResponse(responseCode = "404", description = "Perfil no encontrado")
    })
    @GetMapping("/{idUsuario}/insignias")
    public ResponseEntity<List<InsigniaDTO>> obtenerInsigniasPerfil(
            @Parameter(description = "UUID del usuario asociado al perfil")
            @PathVariable UUID idUsuario) {
        List<InsigniaDTO> insignias = perfilService.obtenerInsigniasPorIdUsuario(idUsuario);
        return ResponseEntity.ok(insignias);
    }

    // TODO: PAGINACION

    // ========== ACTUALIZAR ==========
    @Operation(
        summary = "Actualizar perfil por impacto de donación",
        description = "Permite registrar una nueva donación realizada por el usuario. El servicio procesará el impacto del evento, actualizará las métricas, evaluará las reglas de las misiones y guardará el histórico."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil impactado y actualizado con éxito"),
        @ApiResponse(responseCode = "404", description = "El UUID del usuario especificado no existe en los registros")
    })
    @PutMapping("/donacion/{idUsuario}")
    public ResponseEntity<Boolean> actualizarPerfil(
            @Parameter(description = "UUID del usuario que realizó la donación")
            @PathVariable UUID idUsuario,
            @RequestBody ImpactoDonacionDTO dto) {
        Boolean actualizado = perfilService.actualizarPerfilImpacto(idUsuario, dto);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(actualizado);
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
        PerfilDTO actualizado = perfilService.actualizarDatosPerfil(id, perfil);
        return actualizado == null
               ? ResponseEntity.notFound().build()
               : ResponseEntity.ok(actualizado);
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