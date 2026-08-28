package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionesDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CamionService {
  private final GestorCamiones gestorCamiones;
  private final GestorChoferes gestorChoferes;

  public CamionService(GestorCamiones gestorCamiones, GestorChoferes gestorChoferes) {
    this.gestorCamiones = gestorCamiones;
    this.gestorChoferes = gestorChoferes;
  }

  // --- MÉTODOS CRUD ---
  public CamionesDTO findAll() {
    List<Camion> camiones = gestorCamiones.listarCamiones();
    return new CamionesDTO(camiones.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList()));
  }

  public CamionDTO findById(String patente) {
    Camion camion = gestorCamiones.buscarCamion(patente);
    return convertirADTO(camion);
  }

  public CamionDTO create(CamionDTO dto) {
    Camion nuevoCamion = convertirCamionDTO(dto);
    gestorCamiones.guardarCamion(nuevoCamion);
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
    Chofer nuevoChofer = dto.getIdChofer() != null ? gestorChoferes.buscarChofer(dto.getIdChofer()) : null;
    Camion camionExistente = gestorCamiones.actualizarCamion(patente, dto, nuevoChofer);
    return convertirADTO(camionExistente);
  }

  public void delete(String patente) {
    gestorCamiones.eliminarCamion(patente);
  }

  public String cambiarDisponibilidad(String patente, Map<String, Boolean> body){
    Boolean disponible = body.get("disponible");
    if (disponible != null && disponible) {
      gestorCamiones.marcarDisponible(patente);
      return "Camión marcado como disponible.";
    } else {
      gestorCamiones.marcarOcupado(patente);
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