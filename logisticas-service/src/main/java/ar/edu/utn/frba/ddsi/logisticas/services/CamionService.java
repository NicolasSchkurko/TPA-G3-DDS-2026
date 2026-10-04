package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionesDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CamionService {
  private final GestorCamiones gestorCamiones;
  private final RepositorioCamiones repoCamiones;
  private final RepositorioChoferes repoChoferes;

  public CamionService(GestorCamiones gestorCamiones, RepositorioCamiones repoCamiones, RepositorioChoferes repoChoferes) {
    this.gestorCamiones = gestorCamiones;
    this.repoCamiones = repoCamiones;
    this.repoChoferes = repoChoferes;
  }

  // --- MÉTODOS CRUD ---
  public CamionesDTO findAll() {
    List<Camion> camiones = repoCamiones.findAll();
    return new CamionesDTO(camiones.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList()));
  }

  public CamionDTO findById(String patente) {
    Camion camion = repoCamiones.findById(patente)
            .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));
    return convertirADTO(camion);
  }

  public CamionDTO create(CamionDTO dto) {
    Camion nuevoCamion = convertirCamionDTO(dto);
    repoCamiones.save(nuevoCamion);
    return convertirADTO(nuevoCamion);
  }

  public CamionesDTO createMultiple(CamionesDTO dtos) {
    return new CamionesDTO(dtos.getCamiones().stream()
            .map(this::create).toList());
  }

  public Camion convertirCamionDTO(CamionDTO dto){
    if (dto == null) return null;
    return new Camion(dto.getPatente(), dto.getCapacidadVolumen(),
            dto.getAltura(), dto.getCapacidadCarga(), dto.getDisponible());
  }

  public CamionDTO update(String patente, CamionDTO dto) {
    Chofer nuevoChofer = dto.getIdChofer() != null ? repoChoferes.findById(dto.getIdChofer())
            .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado")): null;
    Camion camionExistente = gestorCamiones.actualizarCamion(patente, nuevoChofer, dto.getCapacidadVolumen(), dto.getAltura(), dto.getCapacidadCarga(), dto.getDisponible());
    return convertirADTO(camionExistente);
  }

  public void delete(String patente) {
    Optional<Camion> camion = repoCamiones.findById(patente);
    if(camion.isPresent()){
      repoCamiones.deleteById(patente);
      throw new IllegalArgumentException("Camión no encontrado");
    }
  }

  public String cambiarDisponibilidad(String patente, Map<String, Boolean> body){
    Boolean disponible = body.get("disponible");
    if (disponible != null && disponible) {
      Camion camion = repoCamiones.findById(patente)
              .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));
      camion.disponible();
      repoCamiones.save(camion);
      return "Camión marcado como disponible.";
    } else {
      Camion camion = repoCamiones.findById(patente)
              .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));
      camion.ocupado();
      repoCamiones.save(camion);
      return "Camión marcado como ocupado.";
    }
  }

  private CamionDTO convertirADTO(Camion camion){
    if (camion == null) return null;
    CamionDTO dto = new CamionDTO();
    if (camion.getChofer() != null) {
      dto.setIdChofer(camion.getChofer().getIdChofer());
    }
    dto.setPatente(camion.getPatente());
    dto.setCapacidadVolumen(camion.getCapacidadVolumen());
    dto.setAltura(camion.getAltura());
    dto.setCapacidadCarga(camion.getCapacidadCarga());
    dto.setDisponible(camion.getDisponible());
    return dto;
  }
}