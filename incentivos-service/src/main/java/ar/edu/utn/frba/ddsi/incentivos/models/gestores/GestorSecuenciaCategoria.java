package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import org.springframework.stereotype.Component;

@Component
public class GestorSecuenciaCategoria {

  private final RepositorioCategorias repositorio;

  public GestorSecuenciaCategoria(RepositorioCategorias repositorio) {
    this.repositorio = repositorio;
  }

  public void desplazarParaCrear(Integer posicionNueva) {
    if (posicionNueva != null) {
      repositorio.desplazarHaciaAbajoDesde(posicionNueva);
    }
  }

  public void desplazarParaActualizar(Integer posAnterior, Integer posNueva, long totalCategorias) {
    if (posNueva == null || posAnterior == null || posNueva.equals(posAnterior)) {
      return;
    }

    if (posNueva >= 1 && posNueva <= totalCategorias) {
      if (posNueva < posAnterior) {
        repositorio.desplazarHaciaAbajo(posNueva, posAnterior - 1);
      } else {
        repositorio.desplazarHaciaArriba(posAnterior + 1, posNueva);
      }
    }
  }

  public void desplazarParaEliminar(Integer posicionLiberada) {
    if (posicionLiberada != null) {
      repositorio.desplazarHaciaArribaDesde(posicionLiberada + 1);
    }
  }
}