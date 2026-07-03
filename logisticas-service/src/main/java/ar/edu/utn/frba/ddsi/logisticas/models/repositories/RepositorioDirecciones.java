package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RepositorioDirecciones {
  private final List<Direccion> direcciones = new ArrayList<>();

  public List<Direccion> findAll() {
    return new ArrayList<>(direcciones);
  }

  public void save(Direccion direccion) {
    if (direccion != null && !direcciones.contains(direccion)) {
      direcciones.add(direccion);
    }
  }

  public void delete(Direccion direccion) {
    direcciones.remove(direccion);
  }
}