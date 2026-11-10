package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionesDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CamionService {
  private final GestorCamiones gestorCamiones;
  private final RepositorioCamiones repoCamiones;
  private final RepositorioChoferes repoChoferes;

  public CamionService(GestorCamiones gestorCamiones, RepositorioCamiones repoCamiones, RepositorioChoferes repoChoferes) {
    this.gestorCamiones = gestorCamiones;
    this.repoCamiones = repoCamiones;
    this.repoChoferes = repoChoferes;
  }

  // --- MÉTODOS CRUD ---
  public Page<CamionDTO> findAll(Pageable pageable) {
    return repoCamiones.findAll(pageable).map(camion -> new CamionDTO().aplicar(camion));
  }

  public CamionDTO findById(String patente) {
    Camion camion = repoCamiones.findById(patente)
            .orElseThrow(() -> new RecursoNoEncontradoException("Camión no encontrado"));
    return new CamionDTO().aplicar(camion);
  }

  public CamionDTO create(CamionDTO dto) {
    Camion nuevoCamion = dto.toDomain();
    repoCamiones.save(nuevoCamion);
    return new CamionDTO().aplicar(nuevoCamion);
  }

  public CamionesDTO createMultiple(CamionesDTO dtos) {
    return new CamionesDTO(dtos.getCamiones().stream()
            .map(this::create).toList());
  }

  public CamionDTO update(String patente, CamionDTO dto) {
    Chofer nuevoChofer = dto.getIdChofer() != null ? repoChoferes.findById(dto.getIdChofer())
            .filter(Chofer::isActivo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Chofer no encontrado")): null;
    Camion camionExistente = repoCamiones.findById(patente)
            .orElseThrow(() -> new RecursoNoEncontradoException("Camión no encontrado"));

    Camion camionActualizado = gestorCamiones.actualizarCamion(camionExistente, nuevoChofer,
            dto.getCapacidadVolumen(), dto.getAltura(), dto.getCapacidadCarga(), dto.getDisponible());
    repoCamiones.save(camionActualizado);
    return new CamionDTO().aplicar(camionActualizado);
  }

  /** Borra el camión: el 404 es para lo que dice (el recurso no estaba). */
  public void delete(String patente) {
    Optional<Camion> camion = repoCamiones.findById(patente);
    if(camion.isPresent()){
      repoCamiones.deleteById(patente);
    }
    else {
      throw new RecursoNoEncontradoException("Camión no encontrado");
    }
  }

  public String cambiarDisponibilidad(String patente, Map<String, Boolean> body){
    if (body == null || !body.containsKey("disponible") || body.get("disponible") == null) {
      throw new IllegalArgumentException("El campo 'disponible' es obligatorio.");
    }

    boolean disponible = body.get("disponible");

    Camion camion = repoCamiones.findById(patente)
            .orElseThrow(() -> new RecursoNoEncontradoException("Camión no encontrado"));
    if (disponible) {
      camion.disponible();
    } else {
      camion.ocupado();
    }
    repoCamiones.save(camion);
    return disponible ? "Camión marcado como disponible." : "Camión marcado como ocupado.";
  }

}
