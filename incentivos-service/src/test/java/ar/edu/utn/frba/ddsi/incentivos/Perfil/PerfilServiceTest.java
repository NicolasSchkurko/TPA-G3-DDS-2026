package ar.edu.utn.frba.ddsi.incentivos.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PerfilServiceTest {

    @Mock
    private RepositorioDonaciones repositorioDonaciones;

    @Mock
    private RepositorioPerfiles repositorioPerfiles;

    @Mock
    private RepositorioRankings repositorioRankings;

    @InjectMocks
    private PerfilService perfilService;

    @Test
    public void obtenerMetricasDonante_calculatesVariation() {
        UUID id = UUID.randomUUID();
        YearMonth ahora = YearMonth.now();
        LocalDate fechaActual = ahora.atDay(1);
        LocalDate fechaAnterior = ahora.minusMonths(1).atDay(1);

        ImpactoDonacion d1 = new ImpactoDonacion("entidadA", 1, fechaActual, "sub", "cat", "ENTREGADA", id);
        ImpactoDonacion d2 = new ImpactoDonacion("entidadA", 1, fechaActual, "sub", "cat", "ENTREGADA", id);
        ImpactoDonacion dPrev = new ImpactoDonacion("entidadB", 1, fechaAnterior, "sub", "cat", "ENTREGADA", id);

        List<ImpactoDonacion> historial = List.of(d1, d2, dPrev);
        when(repositorioDonaciones.buscarDonacionesPorIDUsuario(id)).thenReturn(historial);

        var metrica = perfilService.obtenerMetricasDonante(id);

        assertNotNull(metrica);
        assertEquals(ahora, metrica.getPeriodo());
        assertEquals(100.0, metrica.getVariacionPorcentualDonaciones());
    }

    @Test
    public void obtenerEvolucionHistorica_filtersAndOrders() {
        UUID id = UUID.randomUUID();
        YearMonth ago2 = YearMonth.now().minusMonths(2);
        YearMonth ago1 = YearMonth.now().minusMonths(1);
        LocalDate d1 = ago2.atDay(5);
        LocalDate d2 = ago1.atDay(3);
        LocalDate ignored = YearMonth.now().atDay(2);

        ImpactoDonacion a = new ImpactoDonacion("entA",1,d1,"s","c","ENTREGADA", id);
        ImpactoDonacion b = new ImpactoDonacion("entB",2,d2,"s","c","ENTREGADA", id);
        ImpactoDonacion c = new ImpactoDonacion("entC",1,ignored,"s","c","PENDIENTE", id);

        List<ImpactoDonacion> historial = List.of(a,b,c);
        when(repositorioDonaciones.buscarDonacionesPorIDUsuario(id)).thenReturn(historial);

        List<?> evolucion = perfilService.obtenerEvolucionHistorica(id);
        assertEquals(2, evolucion.size());
        // Orden cronológico ascendente: ago2 then ago1
        assertEquals(ago2, ((ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual)evolucion.get(0)).getPeriodo());
        assertEquals(ago1, ((ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual)evolucion.get(1)).getPeriodo());
    }

    @Test
    public void obtenerTop3DelMes_returnsTop3() {
        YearMonth periodo = YearMonth.now();
        RankingMensual rankingMensual = new RankingMensual(periodo);

        List<Ranking> posiciones = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            PosicionRanking pos = new PosicionRanking(i);
            Ranking r = new Ranking(pos, UUID.randomUUID(), UUID.randomUUID());
            r.setNombreUsuario("user" + i);
            posiciones.add(r);
        }
        rankingMensual.setPosiciones(posiciones);

        when(repositorioRankings.buscarPorPeriodo(periodo)).thenReturn(rankingMensual);

        var top3 = perfilService.obtenerTop3DelMes(periodo);
        assertEquals(3, top3.size());
        assertEquals("user1", top3.get(0).getNombreUsuario());
        assertEquals("user2", top3.get(1).getNombreUsuario());
        assertEquals("user3", top3.get(2).getNombreUsuario());
    }

    @Test
    public void obtenerMisionYInsignias_porID() {
        UUID id = UUID.randomUUID();
        Perfil perfil = new Perfil(id, "nombre");
        Insignia insignia = new Insignia("Ins1", "desc");
        DonacionesExistosas mision = new DonacionesExistosas("m1", insignia, 1);
        perfil.setMisionActual(mision);
        perfil.getInsignias().add(insignia);

        when(repositorioPerfiles.buscarPorIDUsuario(id)).thenReturn(perfil);

        var m = perfilService.obtenerMisionPorID(id);
        assertNotNull(m);
        var insignias = perfilService.obtenerInsigniasPorID(id);
        assertNotNull(insignias);
        assertEquals(1, insignias.size());
    }
}

