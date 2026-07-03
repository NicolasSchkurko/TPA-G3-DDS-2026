package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.services.CamionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/camiones")
public class CamionController {

  private final CamionService camionService;

  public CamionController(CamionService camionService) {
    this.camionService = camionService;
  }

  // Endpoint para registrar camiones disponibles
  @PostMapping
  public ResponseEntity<List<CamionDTO>> registrarCamiones(@RequestBody List<CamionDTO> request){
    try {
      List<Camion> camiones = request.stream()
                                     .map(camionService::nuevoCamion)
                                     .collect(Collectors.toList());

      camionService.guardarCamiones(camiones);

      List<CamionDTO> response = camiones.stream()
                                         .map(camionService::convertirADTO)
                                         .collect(Collectors.toList());

      return new ResponseEntity<>(response, HttpStatus.CREATED);

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}