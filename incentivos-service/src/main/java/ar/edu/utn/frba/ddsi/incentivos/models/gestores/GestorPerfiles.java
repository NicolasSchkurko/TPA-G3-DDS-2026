package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestorPerfiles {
    private final RepositorioPerfiles repositorioPerfiles;

    public GestorPerfiles(RepositorioPerfiles repositorioPerfiles) {
        this.repositorioPerfiles = repositorioPerfiles;
    }

    @Transactional
    public Boolean progresarPerfil(Perfil perfil, ImpactoDonacion donacion) {
        Boolean misionCompletada = perfil.progresarMision(donacion);

        // Al guardar el perfil, Spring Data automáticamente publicará
        // los eventos registrados dentro de la entidad Perfil
        repositorioPerfiles.save(perfil);

        return misionCompletada;
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
                                      if (perfilModificado.getInsignias() != null) {
                                          existente.setInsignias(perfilModificado.getInsignias());
                                      }
                                      if (perfilModificado.getMisionActual() != null) {
                                          existente.setMisionActual(perfilModificado.getMisionActual());
                                      }
                                      if (perfilModificado.getPosicionRanking() != null) {
                                          existente.setPosicionRanking(perfilModificado.getPosicionRanking());
                                      }
                                      // Persistimos los cambios en la base de datos
                                      return repositorioPerfiles.save(existente);
                                  })
                                  .orElse(null);
    }
}