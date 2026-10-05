package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SincronizacionPerfiles {

  private final RepositorioPerfiles repositorioPerfiles;

  // Ya no depende de DonacionClient: antes pedia el contacto del donante una vez por
  // perfil dentro del bucle y con la transaccion abierta (punto 12). Ahora el evento
  // lleva el idUsuario y NotificacionClient resuelve el contacto en AFTER_COMMIT.
  public SincronizacionPerfiles(RepositorioPerfiles repositorioPerfiles) {
    this.repositorioPerfiles = repositorioPerfiles;
  }

  @Transactional
  public void actualizarMisionesPorCambioDeCategoria(
      Categoria categoria,
      Map<UUID, Integer> posicionesAnteriores
  ) {
    List<Perfil> perfiles = repositorioPerfiles.findAllByCategoriaActual(categoria);

    Map<Integer, Mision> misionesPorPosicion = categoria.getCategoriaMisiones().stream()
                                                        .collect(Collectors.toMap(
                                                            cm -> cm.getPosicion(),
                                                            cm -> cm.getMision(),
                                                            (primera, segunda) -> primera
                                                        ));

    for (Perfil perfil : perfiles) {
      if (perfil.getProgresoMisionActual() == null
          || perfil.getProgresoMisionActual().getMision() == null) {
        continue;
      }

      Mision misionActual = perfil.getProgresoMisionActual().getMision();
      Integer posicionAnterior = posicionesAnteriores.get(misionActual.getIdMision());
      if (posicionAnterior == null) {
        continue;
      }

      Mision nuevaMision = misionesPorPosicion.get(posicionAnterior);
      if (nuevaMision == misionActual
          || (nuevaMision != null
          && nuevaMision.getIdMision().equals(misionActual.getIdMision()))) {
        continue;
      }

      // Sin llamada a ningun servicio externo. Antes pedia el contacto con la
      // transaccion abierta y una vez por donante afectado, en un bucle: con 500
      // transaccion abierta y una vez por donante afectado, en un bucle: con 500
      // retenidas (punto 12). Ahora el evento lleva el idUsuario y el listener resuelve
      // el contacto en AFTER_COMMIT.
      perfil.cambiarMision(nuevaMision, misionActual);
    }

    repositorioPerfiles.saveAll(perfiles);
  }

  @Transactional
  public void reiniciarProgresoDeMision(UUID idMision) {
    repositorioPerfiles.reiniciarProgresoDeMision(idMision);
  }
}
