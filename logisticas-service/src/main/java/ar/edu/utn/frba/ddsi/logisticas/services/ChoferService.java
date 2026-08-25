package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferesDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChoferService {
  private final GestorChoferes gestorChoferes;

  public ChoferService(GestorChoferes gestorChoferes) {
    this.gestorChoferes = gestorChoferes;
  }

  // --- MÉTODOS CRUD ---
  public ChoferesDTO findAll() {
    List<Chofer> choferes = gestorChoferes.listarChoferes();
    return new ChoferesDTO(choferes.stream()
            .map(this::convertirAChoferDTO)
            .collect(Collectors.toList()));
  }

  public ChoferDTO findById(UUID id) {
    Chofer chofer = gestorChoferes.buscarChofer(id);
    return convertirAChoferDTO(chofer);
  }

  public ChoferDTO create(ChoferDTO dto) {
    Chofer nuevoChofer = convertirChoferDTO(dto);
    gestorChoferes.guardarChofer(nuevoChofer);
    return convertirAChoferDTO(nuevoChofer);
  }

  private Chofer convertirChoferDTO(ChoferDTO dto){
    if (dto == null) return null;
    return new Chofer(dto.getIdChofer(), dto.getNombre(), dto.isDisponible());
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

  public String cambiarDisponibilidad(UUID id, Map<String, Boolean> body){
    Boolean disponible = body.get("disponible");
    if (disponible != null && disponible) {
      gestorChoferes.marcarDisponible(id);
      return "Chofer marcado como disponible.";
    } else {
      gestorChoferes.marcarOcupado(id);
      return "Chofer marcado como ocupado.";
    }
  }

  // --- MAPPERS ---

  private ChoferDTO convertirAChoferDTO(Chofer chofer){
    if (chofer == null) return null;
    ChoferDTO dto = new ChoferDTO();
    dto.setIdChofer(chofer.getIdChofer());
    dto.setDisponible(chofer.isDisponible());
    return dto;
  }
}