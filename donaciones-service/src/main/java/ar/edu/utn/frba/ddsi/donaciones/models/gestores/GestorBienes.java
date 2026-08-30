package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioBienes;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GestorBienes {

  private RepositorioBienes repositorio;

  public GestorBienes() {
    this.repositorio = new RepositorioBienes();
  }

}

