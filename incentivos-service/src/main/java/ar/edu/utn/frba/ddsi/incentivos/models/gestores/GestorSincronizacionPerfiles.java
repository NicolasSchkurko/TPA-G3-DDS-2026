package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
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
public class GestorSincronizacionPerfiles {

  private final RepositorioPerfiles repositorioPerfiles;
  private final DonacionClient donacionClient;

  public GestorSincronizacionPerfiles(RepositorioPerfiles repositorioPerfiles,
                                      DonacionClient donacionClient) {
    this.repositorioPerfiles = repositorioPerfiles;
    this.donacionClient = donacionClient;
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
                                                            cm -> cm.getMision()
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

      MedioContacto contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());
      perfil.cambiarMision(nuevaMision, misionActual, contacto);
    }

    repositorioPerfiles.saveAll(perfiles);
  }

  @Transactional
  public void reiniciarProgresoDeMision(UUID idMision) {
    repositorioPerfiles.reiniciarProgresoDeMision(idMision);
  }
}