package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/personas") //legacy, deberiamos cambiarlo a donantes
public class DonanteController {

  private final DonanteService donanteService;

  public DonanteController(DonanteService donanteService) {
    this.donanteService = donanteService;
  }

  // CREATE (C)
  @Operation(summary = "Crear/registrar nuevo donante")
  @PostMapping
  public ResponseEntity<?> registrarDonante(@RequestBody PersonaDonanteDTO dto) {
    try {
      PersonaDonanteDTO personaCreada = donanteService.crearPersona(dto);
      return new ResponseEntity<>(personaCreada, HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // READ (R) - Obtener todas
  @Operation(summary = "Obtener todos los donantes")
  @GetMapping
  public ResponseEntity<List<PersonaDonanteDTO>> obtenerTodas() {
    return ResponseEntity.ok(donanteService.listarTodas());
  }

  // READ (R) - Búsqueda por ID
  @Operation(summary = "Obtener donante por id")
  @GetMapping("/{id}")
  public ResponseEntity<PersonaDonanteDTO> obtenerDonantePorId(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(donanteService.buscarPorId(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  // READ (R) - Búsqueda por nombre mediante QueryParam
  @Operation(summary = "Obtener donante por nombre")
  @GetMapping("/buscar")
  public ResponseEntity<PersonaDonanteDTO> buscarDonantePorNombre(@RequestParam String nombre) {
    PersonaDonanteDTO resultado = donanteService.buscarPorNombre(nombre);
    if (resultado == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(resultado);
  }

  // UPDATE (U)
  @Operation(summary = "Actualizar donante por id")
  @PutMapping("/{id}")
  public ResponseEntity<?> actualizarDonante(@PathVariable UUID id, @RequestBody PersonaDonanteDTO dto) {
    try {
      PersonaDonanteDTO actualizada = donanteService.actualizarPersona(id, dto);
      return ResponseEntity.ok(actualizada);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  // DELETE (D)
  @Operation(summary = "Eliminar donante por id")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonante(@PathVariable UUID id) {
    donanteService.eliminarPersona(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Importar donantes desde CSV")
  @PostMapping("/importar")
  public ResponseEntity<String> importarDonanteCSV(
      @RequestPart("file") MultipartFile file,
      @RequestParam("mapeos") String mapeosDtoJson) {
    try {
      if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("El archivo enviado está vacío.");
      }
      ObjectMapper objectMapper = new ObjectMapper();
      List<MapeoCSV> mapeosDominio = objectMapper.readValue(
          mapeosDtoJson,
          new TypeReference<List<MapeoCSV>>() {}
      );
      String mensaje = donanteService.importarDonantes(file, mapeosDominio);
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(mensaje);
    } catch (RuntimeException | JsonProcessingException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // --- ENDPOINTS DE MEDIOS DE CONTACTO ---
  @Operation(summary = "Obtener medios de contacto de donante")
  @GetMapping("/{id}/medios-contacto")
  public ResponseEntity<List<MediosContactoDTO>> obtenerMediosContacto(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(donanteService.obtenerMediosContacto(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "Agregar medio de contacto a donante")
  @PostMapping("/{id}/medios-contacto")
  public ResponseEntity<?> agregarMedioContacto(@PathVariable UUID id, @RequestBody MediosContactoDTO dto) {
    try {
      PersonaDonanteDTO actualizada = donanteService.agregarMedioContacto(id, dto);
      return new ResponseEntity<>(actualizada, HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @Operation(summary = "Eliminar medio de contacto a donante")
  @DeleteMapping("/{id}/medios-contacto")
  public ResponseEntity<?> eliminarMedioContacto(@PathVariable UUID id, @RequestBody MediosContactoDTO dto) {
    try {
      PersonaDonanteDTO actualizada = donanteService.eliminarMedioContacto(id, dto);
      return new ResponseEntity<>(actualizada, HttpStatus.FOUND);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}