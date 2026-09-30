package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
public class GestorSecuenciaCategoria {

  public GestorSecuenciaCategoria() {
  }

  public void desplazarParaCrear(RepositorioCategorias repo, Integer posicionNueva) {
    if (posicionNueva != null) {
      repo.desplazarHaciaAbajoDesde(posicionNueva);
    }
  }

  public void desplazarParaActualizar(RepositorioCategorias repo,Integer posAnterior, Integer posNueva, long totalCategorias) {
    if (posNueva == null || posAnterior == null || posNueva.equals(posAnterior)) {
      return;
    }

    if (posNueva >= 1 && posNueva <= totalCategorias) {
      if (posNueva < posAnterior) {
        repo.desplazarHaciaAbajo(posNueva, posAnterior - 1);
      } else {
        repo.desplazarHaciaArriba(posAnterior + 1, posNueva);
      }
    }
  }

  public void desplazarParaEliminar(RepositorioCategorias repo,Integer posicionLiberada) {
    if (posicionLiberada != null) {
      repo.desplazarHaciaArribaDesde(posicionLiberada + 1);
    }
  }
}