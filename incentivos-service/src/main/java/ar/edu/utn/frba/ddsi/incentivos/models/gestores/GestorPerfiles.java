package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GestorPerfiles {
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioCategorias repositorioCategorias;
  private final DonacionClient donacionClient;

  public GestorPerfiles(RepositorioDonaciones repositorioDonaciones,
                        RepositorioCategorias repositorioCategorias,
                        DonacionClient donacionClient) {
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioCategorias = repositorioCategorias;
    this.donacionClient = donacionClient;
  }

  @Transactional
  public Boolean progresarPerfil(Perfil perfil, ImpactoDonacion donacion) {
    List<ImpactoDonacion> donaciones = List.of();
    Mision misionActual = perfil.getProgresoMisionActual() == null
        ? null
        : perfil.getProgresoMisionActual().getMision();

    if (misionActual != null) {
      donaciones = repositorioDonaciones
          .findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
              perfil.getIdUsuario(),
              misionActual.getIdMision());
    }

    Boolean misionCompletada = perfil.progresarMision(donacion, donaciones);
    if (!misionCompletada || misionActual == null) {
      return misionCompletada;
    }

    this.asignarSiguienteMision(perfil, misionActual);
    return true;
  }

  /**
   * Avanza el recorrido gamificado luego de completar una misión. La insignia
   * ya fue registrada por Perfil.progresarMision, junto con MisionCompletada.
   */
  private void asignarSiguienteMision(Perfil perfil, Mision misionCompletada) {
    Categoria categoriaActual = perfil.getCategoriaActual();
    if (categoriaActual == null) {
      perfil.setProgresoMisionActual(null);
      return;
    }

    Mision siguienteMision = categoriaActual.siguienteMision(misionCompletada);
    if (siguienteMision != null) {
      MedioContacto contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());
      perfil.cambiarMision(siguienteMision, misionCompletada, contacto);
      return;
    }

    Categoria siguienteCategoria = repositorioCategorias
        .obtenerCategoriaSiguiente(categoriaActual);

    if (siguienteCategoria != null) {
      MedioContacto contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());
      perfil.cambiarCategoria(
          siguienteCategoria,
          categoriaActual,
          misionCompletada,
          contacto
      );
      return;
    }

    // Fin del recorrido: no hay transición que notificar. Solo queda el
    // evento MisionCompletada, que comunica la insignia recién obtenida.
    perfil.setProgresoMisionActual(null);
  }

  @Transactional
  public List<Perfil> evaluarProgresosConstantes(List<Perfil> perfilesConMision) {
    perfilesConMision.forEach(perfil -> perfil.verificarProgresoMision(
        repositorioDonaciones.findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
            perfil.getIdUsuario(),
            perfil.getProgresoMisionActual().getMision().getIdMision())
    ));

    return perfilesConMision;
  }
}
