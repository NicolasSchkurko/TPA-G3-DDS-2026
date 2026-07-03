package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CamionService {
  private final RepositorioCamiones repositorioCamiones;
  private final RepositorioChoferes repositorioChoferes;

  public CamionService(RepositorioCamiones repositorioCamiones, RepositorioChoferes repositorioChoferes) {
    this.repositorioCamiones = repositorioCamiones;
    this.repositorioChoferes = repositorioChoferes;
  }

  // --- MÉTODOS CRUD ---
  public List<CamionDTO> findAll() {
    return repositorioCamiones.findAll().stream()
                              .map(this::convertirADTO)
                              .collect(Collectors.toList());
  }

  public CamionDTO findById(String patente) {
    Camion camion = repositorioCamiones.findById(patente)
                                       .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));
    return convertirADTO(camion);
  }

  public CamionDTO create(CamionDTO dto) {
    Camion nuevoCamion = nuevoCamion(dto);
    repositorioCamiones.save(nuevoCamion);
    return convertirADTO(nuevoCamion);
  }

  public List<CamionDTO> createMultiple(List<CamionDTO> dtos) {
  return dtos.stream()
      .map(this::create)
      .collect(Collectors.toList());
  }

  public CamionDTO update(String patente, CamionDTO dto) {
    Camion camionExistente = repositorioCamiones.findById(patente)
                                                .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));

    Chofer nuevoChofer = dto.getIdChofer() != null ? repositorioChoferes.findById(dto.getIdChofer()) : null;

    camionExistente.setChofer(nuevoChofer);
    camionExistente.setCapacidadVolumen(dto.getCapacidadVolumen());
    camionExistente.setAltura(dto.getAltura());
    camionExistente.setCapacidadCarga(dto.getCapacidadCarga());

    repositorioCamiones.save(camionExistente);
    return convertirADTO(camionExistente);
  }

  public void delete(String patente) {
    if (repositorioCamiones.findById(patente).isEmpty()) {
      throw new IllegalArgumentException("Camión no encontrado");
    }
    repositorioCamiones.deleteById(patente);
  }

  // --- MÉTODOS DE NEGOCIO (ESTADOS) ---
  public void marcarDisponible(String patente) {
    Camion camion = repositorioCamiones.findById(patente)
                                       .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));
    camion.disponible();
    repositorioCamiones.save(camion);
  }

  public void marcarOcupado(String patente) {
    Camion camion = repositorioCamiones.findById(patente)
                                       .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));
    camion.ocupado();
    repositorioCamiones.save(camion);
  }

  public void guardarCamiones(List<Camion> camiones){
    if (camiones != null && !camiones.isEmpty()) {
      this.repositorioCamiones.addAll(camiones);
    }
  }

  // --- MAPPERS ---
  public Camion nuevoCamion(CamionDTO dto){
    if (dto == null) return null;
    var chofer = dto.getIdChofer() != null ? repositorioChoferes.findById(dto.getIdChofer()) : null;
    return new Camion(chofer, dto.getPatente(), dto.getCapacidadVolumen(),
                      dto.getAltura(), dto.getCapacidadCarga(), dto.getDisponible());
  }

  public CamionDTO convertirADTO(Camion camion){
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