package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.MapeoCSVDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.services.PersonaService;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/personas") // Modificado a /api/personas para mantener consistencia
public class PersonaController {

  private final PersonaService personaService;

  public PersonaController(PersonaService personaService) {
    this.personaService = personaService;
  }

  // CREATE (C)
  @PostMapping
  public ResponseEntity<?> registrarPersona(@RequestBody PersonaDonanteDTO dto) {
    try {
      PersonaDonanteDTO personaCreada = personaService.crearPersona(dto);
      return new ResponseEntity<>(personaCreada, HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // READ (R) - Obtener todas
  @GetMapping
  public ResponseEntity<List<PersonaDonanteDTO>> obtenerTodas() {
    return ResponseEntity.ok(personaService.listarTodas());
  }

  // READ (R) - Búsqueda por ID (UUID)
  @GetMapping("/{id}")
  public ResponseEntity<PersonaDonanteDTO> obtenerPersonaPorId(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(personaService.buscarPorId(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  // READ (R) - Búsqueda por nombre mediante QueryParam
  @GetMapping("/buscar")
  public ResponseEntity<PersonaDonanteDTO> buscarPersonaPorNombre(@RequestParam String nombre) {
    PersonaDonanteDTO resultado = personaService.buscarPorNombre(nombre);
    if (resultado == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(resultado);
  }

  // UPDATE (U)
  @PutMapping("/{id}")
  public ResponseEntity<?> actualizarPersona(@PathVariable UUID id, @RequestBody PersonaDonanteDTO dto) {
    try {
      PersonaDonanteDTO actualizada = personaService.actualizarPersona(id, dto);
      return ResponseEntity.ok(actualizada);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  // DELETE (D)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarPersona(@PathVariable UUID id) {
    personaService.eliminarPersona(id);
    return ResponseEntity.noContent().build();
  }

  // --- IMPORTACIÓN CSV ---

  @PostMapping("/importar")
  public ResponseEntity<String> importarPersonasCSV(
      @RequestPart("file") MultipartFile file,
      @RequestPart("mapeos") List<MapeoCSVDTO> mapeosDto) {
    try {
      if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("El archivo enviado está vacío.");
      }

      // 1. Convertimos la lista de DTOs provenientes de la web a los MapeoCSV del Dominio
      List<MapeoCSV> mapeosDominio = mapeosDto.stream()
                                              .map(dto -> new MapeoCSV(dto.getCampo(), dto.getNombresColumnas()))
                                              .collect(Collectors.toList());

      // 2. Delegamos la ejecución al servicio utilizando las entidades del dominio
      String mensaje = personaService.importarDonantes(file, mapeosDominio);

      // HttpStatus 202 (Accepted) indica que se aceptó para procesamiento asincrónico
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(mensaje);

    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }


  // --- ENDPOINTS DE MEDIOS DE CONTACTO (quiza haria un controller aparte dsp) ---
  // READ (R)
  @GetMapping("/{id}/medios-contacto")
  public ResponseEntity<List<MediosContactoDTO>> obtenerMediosContacto(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(personaService.obtenerMediosContacto(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
  // CREATE (C)
  @PostMapping("/{id}/medios-contacto")
  public ResponseEntity<?> agregarMedioContacto(@PathVariable UUID id, @RequestBody MediosContactoDTO dto) {
    try {
      PersonaDonanteDTO actualizada = personaService.agregarMedioContacto(id, dto);
      return new ResponseEntity<>(actualizada, HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}