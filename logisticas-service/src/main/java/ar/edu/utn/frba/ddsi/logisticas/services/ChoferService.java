package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferesDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChoferService {
  private final RepositorioChoferes repoChoferes;
  private final RepositorioCamiones repoCamiones;

  public ChoferService(RepositorioChoferes repoChoferes, RepositorioCamiones repoCamiones) {
    this.repoChoferes = repoChoferes;
    this.repoCamiones = repoCamiones;
  }

  // --- MÉTODOS CRUD ---
  public ChoferesDTO findAll() {
    List<Chofer> choferes = repoChoferes.findAll();
    return new ChoferesDTO(choferes.stream()
            .filter(Chofer::isActivo)
            .map(this::convertirAChoferDTO)
            .collect(Collectors.toList()));
  }

  public ChoferDTO findById(UUID id) {
    Chofer chofer = repoChoferes.findById(id)
            .filter(Chofer::isActivo)
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
            .filter(Chofer::isActivo)
            .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado"));

    if (dto.getNombre() != null) {
      choferExistente.setNombre(dto.getNombre());
    }
    if (dto.getDisponible() != null) {
      choferExistente.setDisponible(dto.getDisponible());
    }

    repoChoferes.save(choferExistente);
    return convertirAChoferDTO(choferExistente);
  }

  /**
   * Da de baja lógica al chofer y desvincula el camión si tenía uno asignado.
   * Preserva el registro histórico para trazabilidad de rutas en MongoDB.
   */
  public void delete(UUID id) {
    Chofer chofer = repoChoferes.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado"));

    if (!chofer.isActivo()) {
      throw new IllegalArgumentException("Chofer no encontrado");
    }

    chofer.darDeBaja();
    repoChoferes.save(chofer);

    repoCamiones.findByChofer_IdChofer(id).ifPresent(camion -> {
      camion.eliminarChofer();
      repoCamiones.save(camion);
    });
  }

  public String cambiarDisponibilidad(UUID id, Map<String, Boolean> body){
    if (body == null || !body.containsKey("disponible") || body.get("disponible") == null) {
      throw new IllegalArgumentException("El campo 'disponible' es obligatorio.");
    }

    boolean disponible = body.get("disponible");

    Chofer chofer = repoChoferes.findById(id)
            .filter(Chofer::isActivo)
            .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado"));
    if (disponible) {
      chofer.disponible();
    } else {
      chofer.ocupado();
    }
    repoChoferes.save(chofer);
    return disponible ? "Chofer marcado como disponible." : "Chofer marcado como ocupado.";
  }

  // --- MAPPERS ---

  private ChoferDTO convertirAChoferDTO(Chofer chofer){
    if (chofer == null) return null;
    ChoferDTO dto = new ChoferDTO();
    dto.setIdChofer(chofer.getIdChofer());
    dto.setNombre(chofer.getNombre());
    dto.setDisponible(chofer.isDisponible());
    dto.setActivo(chofer.isActivo());
    return dto;
  }

  private Chofer convertirChoferDTO(ChoferDTO dto){
    if (dto == null) return null;
    boolean esDisponible = dto.getDisponible() == null || dto.getDisponible();
    return new Chofer(UUID.randomUUID(), dto.getNombre(), esDisponible);
  }
}
