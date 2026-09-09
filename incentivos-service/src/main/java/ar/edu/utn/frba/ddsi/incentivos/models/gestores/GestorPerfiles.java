package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GestorPerfiles {
  private final RepositorioPerfiles repositorioPerfiles;
  private final RepositorioDonaciones repositorioDonaciones;

  public GestorPerfiles(RepositorioPerfiles repositorioPerfiles,
                        RepositorioDonaciones repositorioDonaciones) {
    this.repositorioPerfiles = repositorioPerfiles;
    this.repositorioDonaciones = repositorioDonaciones;
  }

  @Transactional
  public Boolean progresarPerfil(Perfil perfil, ImpactoDonacion donacion) {
    List<ImpactoDonacion> donaciones = List.of();
    if (perfil.getProgresoMisionActual() != null
        && perfil.getProgresoMisionActual().getMision() != null) {
      donaciones = repositorioDonaciones
          .findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
              perfil.getIdUsuario(),
              perfil.getProgresoMisionActual().getMision().getIdMision());
    }

    Boolean misionCompletada = perfil.progresarMision(donacion, donaciones);

    repositorioPerfiles.save(perfil);
    repositorioDonaciones.save(donacion);

    return misionCompletada;
  }

  @Transactional
  public void evaluarProgresosConstantes() {
    List<Perfil> perfilesConMision = repositorioPerfiles.findAll()
        .stream()
        .filter(perfil -> perfil.getProgresoMisionActual() != null)
        .filter(perfil -> perfil.getProgresoMisionActual().getMision() != null)
        .filter(perfil -> perfil.getProgresoMisionActual().getMision()
            .getReglaDeProgreso().getConstancia() != null)
        .toList();

    perfilesConMision.forEach(perfil -> perfil.verificarProgresoMision(
        repositorioDonaciones.findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
            perfil.getIdUsuario(),
            perfil.getProgresoMisionActual().getMision().getIdMision())
    ));
    repositorioPerfiles.saveAll(perfilesConMision);
  }

  @Transactional
  public Perfil actualizar(Perfil perfilModificado) {
    if (perfilModificado == null || perfilModificado.getIdUsuario() == null) {
      return null;
    }

    return repositorioPerfiles.findByIdUsuario(perfilModificado.getIdUsuario())
                              .map(existente -> {
                                if (perfilModificado.getNombreUsuario() != null) {
                                  existente.setNombreUsuario(perfilModificado.getNombreUsuario());
                                }
                                if (perfilModificado.getCategoriaActual() != null) {
                                  existente.setCategoriaActual(perfilModificado.getCategoriaActual());
                                }
                                if (perfilModificado.getInsigniasObtenidas() != null) {
                                  existente.setInsigniasObtenidas(perfilModificado.getInsigniasObtenidas());
                                }
                                if (perfilModificado.getProgresoMisionActual() != null) {
                                  existente.setProgresoMisionActual(perfilModificado.getProgresoMisionActual());
                                }

                                return repositorioPerfiles.save(existente);
                              })
                              .orElse(null);
  }
}