package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GestorPerfiles {
  private final RepositorioPerfiles repositorioPerfiles;

  public GestorPerfiles(RepositorioPerfiles repositorioPerfiles) {
    this.repositorioPerfiles = repositorioPerfiles;
  }

  @Transactional
  public Boolean progresarPerfil(Perfil perfil, ImpactoDonacion donacion) {
    Boolean misionCompletada = perfil.progresarMision(donacion);

    repositorioPerfiles.save(perfil);

    return misionCompletada;
  }

  @Transactional
  public void verificarProgresos() {
    List<Perfil> perfilesConMision = repositorioPerfiles.findAll()
        .stream()
        .filter(perfil -> perfil.getProgresoMisionActual() != null)
        .toList();

    perfilesConMision.forEach(Perfil::verificarProgresoMision);
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