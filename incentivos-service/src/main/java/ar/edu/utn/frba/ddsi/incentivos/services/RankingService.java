package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RankingService {
    private final RepositorioRankings repo = RepositorioRankings.getInstance();
    private final RepositorioPerfiles perfiles = RepositorioPerfiles.getInstance();

    // Ejecuta el proceso el día 1 de cada mes a las 00:05 y genera el ranking del mes anterior
    @Scheduled(cron = "0 5 0 1 * ?")
    public void ejecutarRankingMensual() {
        YearMonth periodo = YearMonth.now().minusMonths(1);
        this.generarRankingMensual(periodo);
    }

    private void generarRankingMensual(YearMonth periodo) {
        List<Perfil> todosLosPerfiles = perfiles.listarTodos();

        // 1. Generamos lista de perfiles con su cantidad de misiones en el periodo
        List<Perfil> candidatos = todosLosPerfiles.stream()
                // solo consideramos perfiles con >0 misiones en el periodo
                .filter(perfil -> perfil.getPosicionRanking().getMisionesCumplidasEnPeriodo() != null
                        && perfil.getPosicionRanking().getMisionesCumplidasEnPeriodo() > 0)
                // ordenamos desc por misiones cumplidas
                .sorted((p1, p2) -> Integer.compare(p2.getPosicionRanking().getMisionesCumplidasEnPeriodo(),
                        p1.getPosicionRanking().getMisionesCumplidasEnPeriodo()))
                .toList();

        // 2. Asignamos puestos teniendo en cuenta empates (misiones iguales -> mismo puesto)
        int indice = 0;
        int puestoActual = 1;
        Integer misionesPrevias = null;
        for (Perfil perfil : candidatos) {
            indice++;
            Integer misiones = perfil.getPosicionRanking().getMisionesCumplidasEnPeriodo();
            if (misionesPrevias != null && misiones.equals(misionesPrevias)) {
                perfil.getPosicionRanking().setPuesto(puestoActual);
            } else {
                puestoActual = indice;
                perfil.getPosicionRanking().setPuesto(puestoActual);
            }
            misionesPrevias = misiones;
            // Persistimos el cambio en el repo de perfiles
            perfiles.actualizar(perfil);
        }

        // 3. Construimos la lista de PosicionRanking para el RankingMensual
        List<Ranking> posiciones = new ArrayList<>();
        for (Perfil candidato : candidatos) {
            Ranking ranking = new Ranking(candidato.getPosicionRanking(), candidato.getIdUsuario(), candidato.getIdPerfil());
            posiciones.add(ranking);
        }

        // 4. Construimos y persistimos el objeto de dominio del ranking mensual
        RankingMensual rankingDelMes = new RankingMensual(periodo, posiciones);
        repo.guardar(rankingDelMes);
    }
}
