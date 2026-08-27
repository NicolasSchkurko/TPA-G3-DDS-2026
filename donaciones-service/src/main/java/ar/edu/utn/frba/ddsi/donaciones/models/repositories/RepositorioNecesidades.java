package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RepositorioNecesidades {
  private List<Necesidad> NecesidadesEnMemoria;;

  public RepositorioNecesidades() {
    this.NecesidadesEnMemoria = new ArrayList<>();
  }

  public void guardar(Necesidad Necesidad) {
    if (Necesidad != null) {
      // Aseguramos que tenga un ID si no se inicializó en su constructor
      if (Necesidad.getId() == null) {
        Necesidad.setId(UUID.randomUUID());
      }
      if (buscarPorId(Necesidad.getId()).isPresent()) {
        throw new IllegalArgumentException("Ya existe un Necesidad con el ID: " + Necesidad.getId());
      }
      this.NecesidadesEnMemoria.add(Necesidad);
    }
  }

  public List<Necesidad> obtenerTodos() {
    return new ArrayList<>(this.NecesidadesEnMemoria);
  }

  public Optional<Necesidad> buscarPorId(UUID id) {
    return this.NecesidadesEnMemoria.stream()
                               .filter(a -> a.getId().equals(id))
                               .findFirst();
  }

  public void agregarDonacion(UUID necesidadId, Donacion donacion) {
    Necesidad necesidad = buscarPorId(necesidadId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la Necesidad con ID: " + necesidadId));

    necesidad.registrarDonacionAsignada(donacion);
  }

  public void actualizar(UUID idOriginal, Necesidad NecesidadActualizado) {
    Optional<Necesidad> NecesidadExistente = buscarPorId(idOriginal);
    if (NecesidadExistente.isPresent()) {
      int index = this.NecesidadesEnMemoria.indexOf(NecesidadExistente.get());
      this.NecesidadesEnMemoria.set(index, NecesidadActualizado);
    } else {
      throw new IllegalArgumentException("No se encontró el Necesidad a actualizar.");
    }
  }

  public void eliminarPorId(UUID id) {
    this.NecesidadesEnMemoria.removeIf(a -> a.getId().equals(id));
  }
}
