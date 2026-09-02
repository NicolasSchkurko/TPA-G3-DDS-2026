package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/perfiles") //https:localhost:8001/api/perfiles
@Tag(name = "Gestión de Perfiles e Incentivos", description = "Endpoints para consultar métricas, misiones, insignias y rankings de los perfiles de colaboradores.")
public class PerfilController {
    private final PersonaService personaService;

    public PerfilController(PersonaService personaService) {
        this.personaService = personaService;
    }
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> eliminarPerfil(@PathVariable UUID id) {
//        service.eliminarPerfil(id);
//        return ResponseEntity.noContent().build();
//    }

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
        PerfilDTO perfil = personaService.buscarPorIdUsuario(idUsuario);
        return perfil == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(perfil);
    }

    //https:localhost/perfiles
//permite q Donaciones nos pida crear un Perfil
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
        //los admins se crean en donaciones-service asi q me tiene que pasar el rol
        //si se crea un admin tengo que crearle un perfil
        PerfilDTO nuevo = personaService.crearPerfil(dto);
        if (nuevo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(nuevo);
    }
}
