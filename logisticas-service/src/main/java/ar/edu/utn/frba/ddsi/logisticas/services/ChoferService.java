package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferesDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ChoferService {
  private final RepositorioChoferes repoChoferes;
  private final RepositorioCamiones repoCamiones;

  public ChoferService(RepositorioChoferes repoChoferes, RepositorioCamiones repoCamiones) {
    this.repoChoferes = repoChoferes;
    this.repoCamiones = repoCamiones;
  }

  // --- MÉTODOS CRUD ---
  public Page<ChoferDTO> findAll(Pageable pageable) {
    return repoChoferes.findActivos(pageable).map(chofer -> new ChoferDTO().aplicar(chofer));
  }

  public ChoferDTO findById(UUID id) {
    Chofer chofer = repoChoferes.findById(id)
            .filter(Chofer::isActivo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Chofer no encontrado"));
    return new ChoferDTO().aplicar(chofer);
  }

  public ChoferDTO create(ChoferDTO dto) {
    Chofer nuevoChofer = dto.toDomain();
    repoChoferes.save(nuevoChofer);
    return new ChoferDTO().aplicar(nuevoChofer);
  }

  public ChoferesDTO createMultiple(ChoferesDTO dtos) {
    return new ChoferesDTO(dtos.getChoferes().stream()
            .map(this::create).toList());
  }

  public ChoferDTO update(UUID id, ChoferDTO dto) {
    Chofer choferExistente = repoChoferes.findById(id)
            .filter(Chofer::isActivo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Chofer no encontrado"));

    if (dto.getNombre() != null) {
      choferExistente.setNombre(dto.getNombre());
    }
    if (dto.getDisponible() != null) {
      choferExistente.setDisponible(dto.getDisponible());
    }

    repoChoferes.save(choferExistente);
    return new ChoferDTO().aplicar(choferExistente);
  }

  public void delete(UUID id) {
    Chofer chofer = repoChoferes.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Chofer no encontrado"));

    if (!chofer.isActivo()) {
      throw new RecursoNoEncontradoException("Chofer no encontrado");
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
            .orElseThrow(() -> new RecursoNoEncontradoException("Chofer no encontrado"));
    if (disponible) {
      chofer.disponible();
    } else {
      chofer.ocupado();
    }
    repoChoferes.save(chofer);
    return disponible ? "Chofer marcado como disponible." : "Chofer marcado como ocupado.";
  }

  // --- MAPPERS ---

}
