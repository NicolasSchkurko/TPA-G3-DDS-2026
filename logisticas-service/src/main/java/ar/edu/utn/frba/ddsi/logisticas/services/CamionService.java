package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CamionService {
  private final GestorCamiones gestorCamiones;

  public CamionService(GestorCamiones gestorCamiones) {
    this.gestorCamiones = gestorCamiones;
  }

  // --- MÉTODOS CRUD ---
  public List<CamionDTO> findAll() {
    List<Camion> camiones = gestorCamiones.listarCamiones();
    return camiones.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
  }

  public CamionDTO findById(String patente) {
    Camion camion = gestorCamiones.buscarCamion(patente);
    return convertirADTO(camion);
  }

  public CamionDTO create(CamionDTO dto) {
    Camion nuevoCamion = gestorCamiones.nuevoCamion(dto);
    gestorCamiones.guardarCamion(nuevoCamion);
    return convertirADTO(nuevoCamion);
  }

  public List<CamionDTO> createMultiple(List<CamionDTO> dtos) {
  return dtos.stream()
      .map(this::create)
      .collect(Collectors.toList());
  }

  public CamionDTO update(String patente, CamionDTO dto) {
    Camion camionExistente = gestorCamiones.actualizarCamion(patente, dto);
    return convertirADTO(camionExistente);
  }

  public void delete(String patente) {
    gestorCamiones.eliminarCamion(patente);
  }

  // --- MÉTODOS DE NEGOCIO (ESTADOS) ---
  public void marcarDisponible(String patente) {
    Camion camion = gestorCamiones.buscarCamion(patente);
    camion.disponible();
    gestorCamiones.guardarCamion(camion);
  }

  public void marcarOcupado(String patente) {
    Camion camion = gestorCamiones.buscarCamion(patente);
    camion.ocupado();
    gestorCamiones.guardarCamion(camion);
  }

  public void guardarCamiones(List<Camion> camiones){
    if (camiones != null && !camiones.isEmpty()) {
      camiones.forEach(gestorCamiones::guardarCamion);
    }
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