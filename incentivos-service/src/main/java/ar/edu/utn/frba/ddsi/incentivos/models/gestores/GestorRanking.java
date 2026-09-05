package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
public class GestorRanking {

    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioRankings repositorioRankings;

    public GestorRanking(RepositorioPerfiles repositorioPerfiles, RepositorioRankings repositorioRankings) {
        this.repositorioPerfiles = repositorioPerfiles;
        this.repositorioRankings = repositorioRankings;
    }

    @Transactional
    public RankingMensual generarYPersistirRankingMensual(YearMonth periodo) {
        int mes = periodo.getMonthValue();
        int anio = periodo.getYear();

        List<Object[]> topPerfiles = repositorioPerfiles.calcularRankingMensual(mes, anio);


        RankingMensual rankingDelMes = new RankingMensual(periodo);

        int puestoActual = 1;
        int indiceGral = 1;
        Long misionesPrevias = -1L;

        for (Object[] fila : topPerfiles) {
            Perfil perfil = (Perfil) fila[0];

            Long totalMisiones = ((Number) fila[1]).longValue();

            if (!totalMisiones.equals(misionesPrevias)) {
                puestoActual = indiceGral;
            }

            Ranking posicion = new Ranking(
                rankingDelMes,
                perfil.getIdUsuario(),
                perfil.getNombreUsuario(),
                puestoActual,
                totalMisiones
            );

            rankingDelMes.agregarPosicion(posicion);

            misionesPrevias = totalMisiones;
            indiceGral++;
        }


        return repositorioRankings.save(rankingDelMes);
    }
}