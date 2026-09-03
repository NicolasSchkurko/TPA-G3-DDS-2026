package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.events.GenerarRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.events.ResultadosRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import java.time.YearMonth;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GestorRanking {
    private final RepositorioRankings repo;
    private final ApplicationEventPublisher eventPublisher;
    private final RepositorioPerfiles repositorio;

    public GestorRanking(RepositorioRankings rankings, ApplicationEventPublisher eventPublisher, RepositorioPerfiles repositorio) {
        this.repo = rankings;
        this.eventPublisher = eventPublisher;
        this.repositorio = repositorio;
    }

    @EventListener
    public void generarRankingMensual(GenerarRanking event) {
        // lista de perfiles con su cantidad de misiones en el periodo
        List<Perfil> candidatos = event.perfiles();

        // Asignamos puestos teniendo en cuenta empates (misiones iguales -> mismo puesto)
        int indice = 0;
        int puestoActual = 1;
        Integer misionesPrevias = null;
        for (Perfil candidato : candidatos) {
            indice++;
            Integer misiones = candidato.getPosicionRanking().getMisionesCumplidasEnPeriodo();
            if (misionesPrevias != null && misiones.equals(misionesPrevias)) {
                candidato.getPosicionRanking().setPuesto(puestoActual);
            } else {
                puestoActual = indice;
                candidato.getPosicionRanking().setPuesto(puestoActual);
            }
            misionesPrevias = misiones;
        }

        // Construimos la lista de PosicionRanking para el RankingMensual
        List<Ranking> posiciones = new ArrayList<>();
        for (Perfil candidato : candidatos) {
            Ranking ranking = new Ranking(candidato.getPosicionRanking(), candidato.getIdUsuario(), candidato.getIdPerfil());
            posiciones.add(ranking);
        }

        // Construimos y persistimos el objeto de dominio del ranking mensual
        RankingMensual rankingDelMes = new RankingMensual(event.periodo(), posiciones);
        repo.guardar(rankingDelMes);

        eventPublisher.publishEvent(
                new ResultadosRanking(
                        rankingDelMes.getPosiciones()
                )
        );
    }

    public void generarRankingMensual(YearMonth periodo){
        // lista de perfiles con su cantidad de misiones en el periodo
        List<Perfil> candidatos = repositorio.listarTodos().stream()
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
}
