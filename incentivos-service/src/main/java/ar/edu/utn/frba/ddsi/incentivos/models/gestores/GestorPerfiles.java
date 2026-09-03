package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.events.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class GestorPerfiles {
    private final RepositorioPerfiles repositorio;
    private final ApplicationEventPublisher eventPublisher;

    public GestorPerfiles(RepositorioPerfiles repositorio, ApplicationEventPublisher eventPublisher) {
        this.repositorio = repositorio;
        this.eventPublisher = eventPublisher;
    }



    public Perfil progresarPerfil(UUID idUsuario, ImpactoDonacion donacion) {
        Perfil perfil = repositorio.buscarPorIDUsuario(idUsuario);
        if (perfil == null) return null;

        Mision misionAnterior = perfil.getMisionActual();
        Categoria categoriaActual = perfil.getCategoriaActual();
        boolean misionCompletada = perfil.progresarMision(donacion);
        repositorio.actualizar(perfil);

        if (categoriaActual.esUltimaMision(misionAnterior)) {
            eventPublisher.publishEvent(
                    new UltimaMisionCategoria(
                            categoriaActual.getIdCategoria(),
                            perfil.getIdPerfil()
                    )
            );
        } else {
            Mision misionNueva = categoriaActual.siguienteMision(misionAnterior);
            perfil.setMisionActual(misionNueva);
            repositorio.actualizar(perfil);
            eventPublisher.publishEvent(
                    new MisionCambiada(misionAnterior.getNombreMision(),
                            misionAnterior.getInsigniaObjetivo().getNombre(),
                            perfil.getNombreUsuario(),
                            perfil.getIdUsuario(),
                            perfil.getMisionActual().getNombreMision()
                    )
            );
        }

        return perfil;
    }


    @EventListener
    public void actualizarPerfil(CategoriaCambiada event) {
        if (event.categoriaNueva() == null) {
            return;
        }

        Perfil perfil = repositorio.buscarPorIDPerfil(event.idPerfil());

        Mision misionAnterior = perfil.getMisionActual();

        perfil.setCategoriaActual(event.categoriaNueva());
        perfil.setMisionActual(event.categoriaNueva().primeraMision());
        repositorio.actualizar(perfil);

        eventPublisher.publishEvent(
                new MisionCambiada(misionAnterior.getNombreMision(),
                        misionAnterior.getInsigniaObjetivo().getNombre(),
                        perfil.getNombreUsuario(),
                        perfil.getIdUsuario(),
                        perfil.getMisionActual().getNombreMision()
                )
        );

        eventPublisher.publishEvent(
                new CategoriaNuevaPublicar(
                        event.categoriaAnterior().getNombre(),
                        perfil.getCategoriaActual().getNombre(),
                        perfil.getNombreUsuario(),
                        perfil.getmedio
                )
        );
    }
}
