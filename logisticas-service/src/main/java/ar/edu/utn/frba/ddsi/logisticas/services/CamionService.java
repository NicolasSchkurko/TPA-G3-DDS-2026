package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CamionService {
  private final RepositorioCamiones repositorioCamiones;
  private final RepositorioChoferes repositorioChoferes;

  public CamionService(RepositorioCamiones repositorioCamiones, RepositorioChoferes repositorioChoferes) {
    this.repositorioCamiones = repositorioCamiones;
    this.repositorioChoferes = repositorioChoferes;
  }

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

  public void guardarCamiones(List<Camion> camiones){
    if (camiones != null && !camiones.isEmpty()) {
      this.repositorioCamiones.addAll(camiones);
    }
  }
}