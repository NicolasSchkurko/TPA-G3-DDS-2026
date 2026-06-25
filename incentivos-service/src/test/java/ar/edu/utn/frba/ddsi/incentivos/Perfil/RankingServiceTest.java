package ar.edu.utn.frba.ddsi.incentivos.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import ar.edu.utn.frba.ddsi.incentivos.scheduler.RankingScheduler;
import ar.edu.utn.frba.ddsi.incentivos.services.RankingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RankingServiceTest {

    @Mock
    private RepositorioPerfiles perfiles;

    @Mock
    private RepositorioRankings repo;

    @Mock
    private RankingScheduler rankingScheduler;

    @InjectMocks
    private RankingService rankingService;

    @Test
    public void ejecutarRankingMensual_generatesRankingAndAssignsPositions() {
        Perfil p1 = new Perfil(UUID.randomUUID(), "u1");
        Perfil p2 = new Perfil(UUID.randomUUID(), "u2");
        Perfil p3 = new Perfil(UUID.randomUUID(), "u3");

        // Set misiones cumplidas to create a tie between p1 and p2
        p1.getPosicionRanking().setMisionesCumplidasEnPeriodo(3);
        p2.getPosicionRanking().setMisionesCumplidasEnPeriodo(3);
        p3.getPosicionRanking().setMisionesCumplidasEnPeriodo(1);

        List<Perfil> todos = List.of(p1, p2, p3);
        when(perfiles.listarTodos()).thenReturn(todos);

        // Run scheduled method (it uses YearMonth.now().minusMonths(1))
        rankingScheduler.ejecutarRankingMensual();

        ArgumentCaptor<RankingMensual> captor = ArgumentCaptor.forClass(RankingMensual.class);
        verify(repo).guardar(captor.capture());
        RankingMensual generado = captor.getValue();
        assertEquals(YearMonth.now().minusMonths(1), generado.getPeriodo());
        assertEquals(3, generado.getPosiciones().size());

        // Verify puestos: tie for first -> puestos [1,1,3]
        List<Integer> puestos = new ArrayList<>();
        for (Ranking r : generado.getPosiciones()) {
            puestos.add(r.getPosicionRanking().getPuesto());
        }
        assertEquals(List.of(1,1,3), puestos);

        // Ensure perfiles.actualizar fue invocado para cada candidato
        verify(perfiles, atLeastOnce()).actualizar(any(Perfil.class));
    }
}

