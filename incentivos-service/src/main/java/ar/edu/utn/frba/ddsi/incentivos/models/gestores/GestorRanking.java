package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.events.GenerarRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.events.ResultadosRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GestorRanking {
    private final RepositorioRankings repo;
    private final ApplicationEventPublisher eventPublisher;

    public GestorRanking(RepositorioRankings rankings, ApplicationEventPublisher eventPublisher) {
        this.repo = rankings;
        this.eventPublisher = eventPublisher;
    }

    public RankingMensual obtenerRanking(UUID idRanking){
        return repo.buscarPorId(idRanking);
    }

    public RankingMensual obtenerTop3(UUID idRanking){
        RankingMensual rank = repo.buscarPorId(idRanking);

        List<Ranking> top3 =  rank.getPosiciones().stream()
                .limit(3) // Nos quedamos solo con los 3 primeros elementos de la lista ya ordenada
                .toList();

        rank.setPosiciones(top3);

        return rank;
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
}
