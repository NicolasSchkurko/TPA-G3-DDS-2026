package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.PersonaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/personas")
public class PersonaController {

  private final PersonaService personaService;

  public PersonaController(PersonaService personaService) {
    this.personaService = personaService;
  }

  // Ruta para crear: POST /personas
  @PostMapping
  public ResponseEntity<String> registrarPersona(@RequestBody PersonaDonanteDTO dto) {
    try {
      personaService.crearPersona(dto);
      return new ResponseEntity<>("Persona registrada con éxito", HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // Ruta para buscar: GET /personas/buscar?nombre=Juan Perez
  @GetMapping
  public ResponseEntity<PersonaDonanteDTO> buscarPersona(@RequestParam String nombre) {
    PersonaDonanteDTO resultado = personaService.buscarPorNombre(nombre);

    if (resultado == null) {
      return ResponseEntity.notFound().build(); // Devuelve 404
    }

    return ResponseEntity.ok(resultado); // Devuelve 200 con el JSON
  }
}