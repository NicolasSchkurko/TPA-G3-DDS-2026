package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.services.PersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/perfiles")
@Tag(name = "Integración de Perfiles (Inter-servicio)", description = "Endpoints utilizados principalmente por el microservicio de Donaciones para sincronizar altas y modificaciones de actividad.")
public class PersonaController {
    private final PersonaService personaService;

    public PersonaController(PersonaService perfilService) {
        this.personaService = perfilService;
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
        try {
            PerfilDTO nuevo = personaService.crearPerfil(dto);
            return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    //https:localhost/perfiles/{idUsuario}
//permite q Donaciones nos pida actualizarPerfil ante una donacion
    @Operation(
            summary = "Actualizar perfil por impacto de donación",
            description = "Permite registrar una nueva donación realizada por el usuario. El servicio procesará el impacto del evento, actualizará las métricas, evaluará las reglas de las misiones y guardará el histórico."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil impactado y actualizado con éxito"),
            @ApiResponse(responseCode = "404", description = "El UUID del usuario especificado no existe en los registros")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PerfilDTO> actualizarPerfil(@Parameter(description = "UUID del usuario que realizó la donación", example = "123e4567-e89b-12d3-a456-426614174000")
                                                      @PathVariable UUID id,
                                                      @RequestBody ImpactoDonacionDTO dto) {
        //Recibe donacion, actualizar perfil y guardan en repo de donaciones
        try {
            PerfilDTO actualizado = personaService.actualizarPerfil(id, dto);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}