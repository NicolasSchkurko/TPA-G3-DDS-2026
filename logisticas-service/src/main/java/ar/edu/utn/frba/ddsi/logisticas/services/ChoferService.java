package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChoferService {
  private final RepositorioChoferes repositorioChoferes;

  public ChoferService(RepositorioChoferes repositorioChoferes) {
    this.repositorioChoferes = repositorioChoferes;
  }

  public Chofer convertirChoferDTO(ChoferDTO dto){
    if (dto == null) return null;
    return new Chofer(dto.getIdChofer(), dto.isDisponible());
  }

  public ChoferDTO convertirAChoferDTO(Chofer chofer){
    if (chofer == null) return null;
    ChoferDTO dto = new ChoferDTO();
    dto.setIdChofer(chofer.getIdChofer());
    dto.setDisponible(chofer.isDisponible());
    return dto;
  }

  public void guardarChoferes(List<Chofer> choferes){
    if (choferes != null && !choferes.isEmpty()) {
      this.repositorioChoferes.addAll(choferes);
    }
  }
}