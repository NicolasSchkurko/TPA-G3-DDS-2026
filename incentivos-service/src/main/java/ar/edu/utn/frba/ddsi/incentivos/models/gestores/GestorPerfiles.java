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

    public void verificarProgresos() {
        repositorio.listarTodos().stream()
                .filter(perfil -> perfil.getMisionActual().getReglaDeProgreso().getConstancia() != null)
                .forEach(Perfil::verificarProgresoMision);
    }

    public Mision obtenerMisionPerfil(UUID idUsuario){
        Perfil perfil = repositorio.buscarPorIDUsuario(idUsuario);

        return perfil.getMisionActual();
    }

    public List<Insignia> obtenerInsigniasPerfil(UUID idUsuario){
        Perfil perfil = repositorio.buscarPorIDUsuario(idUsuario);

        return perfil.getInsignias();
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

    public void generarRankingMensual(YearMonth periodo){
        // lista de perfiles con su cantidad de misiones en el periodo
        List<Perfil> candidatos = repositorio.listarTodos().stream()
                // solo consideramos perfiles con >0 misiones en el periodo
                .filter(perfil -> perfil.getPosicionRanking().getMisionesCumplidasEnPeriodo() != null
                        && perfil.getPosicionRanking().getMisionesCumplidasEnPeriodo() > 0)
                // ordenamos desc por misiones cumplidas
                .sorted((p1, p2) -> Integer.compare(p2.getPosicionRanking().getMisionesCumplidasEnPeriodo(),
                        p1.getPosicionRanking().getMisionesCumplidasEnPeriodo()))
                .toList();

        eventPublisher.publishEvent(
                new GenerarRanking(
                        periodo,
                        candidatos
                )
        );
    }

    @EventListener
    public void actualizarPosicionesRanking(ResultadosRanking event){
        List<Ranking> posiciones = event.posiciones();

        for(Ranking pos : posiciones){
            Perfil p = repositorio.buscarPorIDPerfil(pos.getIdPerfil());
            p.setPosicionRanking(pos.getPosicionRanking());
            repositorio.actualizar(p);
        }
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
                        perfil.getIdUsuario()
                )
        );
    }
}
