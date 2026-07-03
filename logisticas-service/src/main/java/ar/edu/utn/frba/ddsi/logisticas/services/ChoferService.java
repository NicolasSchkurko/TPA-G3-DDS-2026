package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChoferService {
  private final RepositorioChoferes repositorioChoferes;

  public ChoferService(RepositorioChoferes repositorioChoferes) {
    this.repositorioChoferes = repositorioChoferes;
  }

  // --- MÉTODOS CRUD ---
  public List<ChoferDTO> findAll() {
    return repositorioChoferes.findAll().stream()
                              .map(this::convertirAChoferDTO)
                              .collect(Collectors.toList());
  }

  public ChoferDTO findById(UUID id) {
    Chofer chofer = repositorioChoferes.findById(id);
    if (chofer == null) throw new IllegalArgumentException("Chofer no encontrado");
    return convertirAChoferDTO(chofer);
  }

  public ChoferDTO create(ChoferDTO dto) {
    Chofer nuevoChofer = convertirChoferDTO(dto);
    repositorioChoferes.save(nuevoChofer);
    return convertirAChoferDTO(nuevoChofer);
  }

  public ChoferDTO update(UUID id, ChoferDTO dto) {
    Chofer choferExistente = repositorioChoferes.findById(id);
    if (choferExistente == null) throw new IllegalArgumentException("Chofer no encontrado");

    choferExistente.setDisponible(dto.isDisponible());
    repositorioChoferes.save(choferExistente);
    return convertirAChoferDTO(choferExistente);
  }

  public void delete(UUID id) {
    if (repositorioChoferes.findById(id) == null) throw new IllegalArgumentException("Chofer no encontrado");
    repositorioChoferes.deleteById(id);
  }

  // --- MÉTODOS DE NEGOCIO (ESTADOS) ---
  public void marcarDisponible(UUID id) {
    Chofer chofer = repositorioChoferes.findById(id);
    if (chofer == null) throw new IllegalArgumentException("Chofer no encontrado");
    chofer.disponible();
    repositorioChoferes.save(chofer);
  }

  public void marcarOcupado(UUID id) {
    Chofer chofer = repositorioChoferes.findById(id);
    if (chofer == null) throw new IllegalArgumentException("Chofer no encontrado");
    chofer.ocupado();
    repositorioChoferes.save(chofer);
  }

  public void guardarChoferes(List<Chofer> choferes){
    if (choferes != null && !choferes.isEmpty()) {
      this.repositorioChoferes.addAll(choferes);
    }
  }

  // --- MAPPERS ---
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
}