package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferesDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChoferService {
  private final RepositorioChoferes repoChoferes;

  public ChoferService(RepositorioChoferes repoChoferes) {
    this.repoChoferes = repoChoferes;
  }

  // --- MÉTODOS CRUD ---
  public ChoferesDTO findAll() {
    List<Chofer> choferes = repoChoferes.findAll();
    return new ChoferesDTO(choferes.stream()
            .map(this::convertirAChoferDTO)
            .collect(Collectors.toList()));
  }

  public ChoferDTO findById(UUID id) {
    Chofer chofer = repoChoferes.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado"));
    return convertirAChoferDTO(chofer);
  }

  public ChoferDTO create(ChoferDTO dto) {
    Chofer nuevoChofer = convertirChoferDTO(dto);
    repoChoferes.save(nuevoChofer);
    return convertirAChoferDTO(nuevoChofer);
  }

  public ChoferesDTO createMultiple(ChoferesDTO dtos) {
    return new ChoferesDTO(dtos.getChoferes().stream()
            .map(this::create).toList());
  }

  public ChoferDTO update(UUID id, ChoferDTO dto) {
    Chofer choferExistente = repoChoferes.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado"));
    choferExistente.setNombre(dto.getNombre());
    choferExistente.setDisponible(dto.isDisponible());
    repoChoferes.save(choferExistente);
    return convertirAChoferDTO(choferExistente);
  }

  public void delete(UUID id) {
    Optional<Chofer> chofer = repoChoferes.findById(id);
    if(chofer.isPresent()){
      repoChoferes.deleteById(id);
    }
    else {
      throw new IllegalArgumentException("Chofer no encontrado");
    }
  }

  public String cambiarDisponibilidad(UUID id, Map<String, Boolean> body){
    Boolean disponible = body.get("disponible");
    if (disponible != null && disponible) {
      Chofer chofer  = repoChoferes.findById(id)
              .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado"));
      chofer.disponible();
      repoChoferes.save(chofer);
      return "Chofer marcado como disponible.";
    } else {
      Chofer chofer = repoChoferes.findById(id)
              .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado"));
      chofer.ocupado();
      repoChoferes.save(chofer);
      return "Chofer marcado como ocupado.";
    }
  }

  // --- MAPPERS ---

  private ChoferDTO convertirAChoferDTO(Chofer chofer){
    if (chofer == null) return null;
    ChoferDTO dto = new ChoferDTO();
    dto.setIdChofer(chofer.getIdChofer());
    dto.setNombre(chofer.getNombre());
    dto.setDisponible(chofer.isDisponible());
    return dto;
  }

  private Chofer convertirChoferDTO(ChoferDTO dto){
    if (dto == null) return null;
    return new Chofer(UUID.randomUUID(), dto.getNombre(), dto.isDisponible());
  }
}