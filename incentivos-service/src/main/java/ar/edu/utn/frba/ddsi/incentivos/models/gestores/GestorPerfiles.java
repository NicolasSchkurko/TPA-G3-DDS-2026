package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.events.UltimaMisionCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GestorPerfiles {
    private final RepositorioPerfiles repositorio;
    private final ApplicationEventPublisher eventPublisher;

    public GestorPerfiles(RepositorioPerfiles repositorio, ApplicationEventPublisher eventPublisher) {
        this.repositorio = repositorio;
        this.eventPublisher = eventPublisher;
    }

    public void verificarProgresos() {
        repositorio.listarTodos().stream()
                .filter(perfil -> perfil.getMisionActual().getReglaDeProgreso().getConstancia() != null)
                .forEach(Perfil::verificarProgresoMision);
    }

    public Perfil crearPerfil(Perfil nuevo) {
        if (repositorio.buscarPorIDUsuario(nuevo.getIdUsuario()) != null) return null;
        repositorio.agregarPerfil(nuevo);
        return repositorio.buscarPorIDPerfil(nuevo.getIdPerfil());
    }

    public Perfil progresarPerfil(UUID idUsuario, ImpactoDonacion donacion) {
        Perfil perfil = repositorio.buscarPorIDUsuario(idUsuario);
        if (perfil == null) return null;

        Mision misionAnterior = perfil.getMisionActual();
        Categoria categoriaActual = perfil.getCategoriaActual();
        boolean misionCompletada = perfil.progresarMision(donacion);
        repositorio.actualizar(perfil);

        if (!misionCompletada) return perfil;

        if (categoriaActual.esUltimaMision(misionAnterior)) {
            eventPublisher.publishEvent(
                    new UltimaMisionCategoria(categoriaActual.getIdCategoria(),
                            perfil)
            );
        } else {
            Mision misionNueva = categoriaActual.siguienteMision(misionAnterior);
            perfil.setMisionActual(misionNueva);
            repositorio.actualizar(perfil);
            eventPublisher.publishEvent(
                    new MisionCambiada(misionAnterior.getNombreMision(),
                            misionAnterior.getInsigniaObjetivo().getNombre(),
                            perfil.getNombreUsuario(),
                            perfil.getMisionActual().getNombreMision()
                    )
            );
        }

        return perfil;
    }

//    eventPublisher.publishEvent(new CategoriaCambiadaEvent(
//            categoriaAnterior.getNombre(), perfil)
//            );

    public Perfil conseguirPerfil(UUID idUsuario) {
        return repositorio.buscarPorIDUsuario(idUsuario);
    }

    public Perfil actualizarPerfil(Perfil perfil) {
        return repositorio.actualizar(perfil);
    }
}
