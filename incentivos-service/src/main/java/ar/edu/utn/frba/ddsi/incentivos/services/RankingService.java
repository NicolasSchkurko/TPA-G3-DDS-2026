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

        // 1. Generamos posiciones contando las insignias obtenidas en el periodo
        List<PosicionRanking> posiciones = todosLosPerfiles.stream()
                .map(perfil -> {
                    int misionesEnPeriodo = (int) perfil.getInsignias().stream()
                            .filter(insignia -> insignia.getFechaObtencion() != null &&
                                    YearMonth.from(insignia.getFechaObtencion()).equals(periodo))
                            .count();
                    return new PosicionRanking(null, perfil.getIdPerfil(), perfil.getIdUsuario(),
                            perfil.getNombreUsuario(), misionesEnPeriodo);
                })
                // solo consideramos perfiles con >0 misiones en el periodo
                .filter(p -> p.getMisionesCumplidasEnPeriodo() != null && p.getMisionesCumplidasEnPeriodo() > 0)
                // ordenamos desc por misiones cumplidas
                .sorted((p1, p2) -> Integer.compare(p2.getMisionesCumplidasEnPeriodo(), p1.getMisionesCumplidasEnPeriodo()))
                .toList();

        // 1.b Asignamos puestos teniendo en cuenta empates (misiones iguales -> mismo puesto)
        int indice = 0;
        int puestoActual = 1;
        Integer misionesPrevias = null;
        for (PosicionRanking p : posiciones) {
            indice++;
            Integer misiones = p.getMisionesCumplidasEnPeriodo();
            if (misionesPrevias != null && misiones.equals(misionesPrevias)) {
                p.setPuesto(puestoActual); // mismo puesto que el anterior
            } else {
                puestoActual = indice; // salto según índice
                p.setPuesto(puestoActual);
            }
            misionesPrevias = misiones;
        }

        // 2. Asignamos la posición calculada a cada Perfil en el repositorio
        perfiles.asignarPosicionesRanking(posiciones);

        // 3. Construimos y persistimos el objeto de dominio del ranking mensual
        RankingMensual rankingDelMes = new RankingMensual(periodo, posiciones);
        repo.guardar(rankingDelMes);
    }
}
