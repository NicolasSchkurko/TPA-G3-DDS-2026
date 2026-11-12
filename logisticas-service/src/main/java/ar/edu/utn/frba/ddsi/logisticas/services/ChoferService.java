package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferesDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ChoferService {
  private final RepositorioChoferes repoChoferes;
  private final GestorChoferes gestorChoferes;

  public ChoferService(RepositorioChoferes repoChoferes, GestorChoferes gestorChoferes) {
    this.repoChoferes = repoChoferes;
    this.gestorChoferes = gestorChoferes;
  }

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
    Chofer actualizado = gestorChoferes.actualizar(id, dto.getNombre(), dto.getEmail(), dto.getDisponible());
    return new ChoferDTO().aplicar(actualizado);
  }

  public void delete(UUID id) {
    gestorChoferes.darDeBaja(id);
  }

  public String cambiarDisponibilidad(UUID id, Map<String, Boolean> body){
    if (body == null || !body.containsKey("disponible") || body.get("disponible") == null) {
      throw new IllegalArgumentException("El campo 'disponible' es obligatorio.");
    }

    boolean disponible = body.get("disponible");
    gestorChoferes.cambiarDisponibilidad(id, disponible);
    return disponible ? "Chofer marcado como disponible." : "Chofer marcado como ocupado.";
  }
}
