package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChoferService {
  private final GestorChoferes gestorChoferes;

  public ChoferService(GestorChoferes gestorChoferes) {
    this.gestorChoferes = gestorChoferes;
  }

  // --- MÉTODOS CRUD ---
  public List<ChoferDTO> findAll() {
    List<Chofer> choferes = gestorChoferes.listarChoferes();
    return choferes.stream()
            .map(this::convertirAChoferDTO)
            .collect(Collectors.toList());
  }

  public ChoferDTO findById(UUID id) {
    Chofer chofer = gestorChoferes.buscarChofer(id);
    return convertirAChoferDTO(chofer);
  }

  public ChoferDTO create(ChoferDTO dto) {
    Chofer nuevoChofer = gestorChoferes.nuevoChofer(dto);
    gestorChoferes.guardarChofer(nuevoChofer);
    return convertirAChoferDTO(nuevoChofer);
  }

  public ChoferDTO update(UUID id, ChoferDTO dto) {
    Chofer choferExistente = gestorChoferes.buscarChofer(id);
    choferExistente.setDisponible(dto.isDisponible());
    gestorChoferes.guardarChofer(choferExistente);
    return convertirAChoferDTO(choferExistente);
  }

  public void delete(UUID id) {
    gestorChoferes.eliminarChofer(id);
  }

  // --- MÉTODOS DE NEGOCIO (ESTADOS) ---
  public void marcarDisponible(UUID id) {
    Chofer chofer = gestorChoferes.buscarChofer(id);
    chofer.disponible();
    gestorChoferes.guardarChofer(chofer);
  }

  public void marcarOcupado(UUID id) {
    Chofer chofer = gestorChoferes.buscarChofer(id);
    chofer.ocupado();
    gestorChoferes.guardarChofer(chofer);
  }

  public void guardarChoferes(List<Chofer> choferes){
    if (choferes != null && !choferes.isEmpty()) {
      choferes.forEach(gestorChoferes::guardarChofer);
    }
  }

  // --- MAPPERS ---

  public ChoferDTO convertirAChoferDTO(Chofer chofer){
    if (chofer == null) return null;
    ChoferDTO dto = new ChoferDTO();
    dto.setIdChofer(chofer.getIdChofer());
    dto.setDisponible(chofer.isDisponible());
    return dto;
  }
}