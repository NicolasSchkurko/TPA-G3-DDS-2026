package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

/**
 * El snapshot del ranking tiene que ser completo, y el recorte va al responder (punto 2).
 *
 * <p>El bug era de los que no se ven: el service guardaba las 10 primeras posiciones del mes
 * y el endpoint del podio aceptaba un {@code limite} mayor. Pedir 50 devolvía 10, con un 200 y
 * sin ningún aviso. Peor que un error, porque el cliente no tenía forma de saber que le
 * faltaban 40.
 *
 * <p>Las dos mitades se comprueban por separado: que la generación no corte, y que el corte
 * al responder respete lo que hay guardado.
 */
@DisplayName("Punto 2: el ranking se persiste completo y el limite recorta la respuesta")
class RankingServiceSnapshotCompletoTest {

    private final RepositorioRankings repoRankings = mock(RepositorioRankings.class);
    private final RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
    private final ValidadorAdmin validador = mock(ValidadorAdmin.class);
    private final RankingService service =
            new RankingService(repoRankings, repoPerfiles, validador);

    private final UUID idRanking = UUID.randomUUID();

    @Nested
    @DisplayName("la generación no recorta")
    class LaGeneracionNoRecorta {

        @Test
        @DisplayName("la consulta del ranking va sin corte de filas")
        void laConsultaVaSinCorte() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());
            when(repoRankings.save(any())).thenAnswer(invoc -> invoc.getArgument(0));
            when(repoPerfiles.calcularRankingMensual(any(), any(), captor.capture()))
                    .thenReturn(List.of());

            service.crearRankingMensual(UUID.randomUUID(), YearMonth.now().minusMonths(1));

            assertThat(captor.getValue().isPaged())
                    .as("""
                    Con PageRequest.of(0, 10) la base devolvía diez filas y el snapshot
                    guardaba esas diez. Un Pageable sin corte es lo que hace que el snapshot
                    sea completo.
                    """)
                    .isFalse();
        }

        @Test
        @DisplayName("si no hay corte, la consulta devuelve todas las filas del período")
        void seGuardanTodasLasFilasDelPeriodo() {
            when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());

            when(repoRankings.save(any())).thenAnswer(invoc -> invoc.getArgument(0));
            when(repoPerfiles.calcularRankingMensual(any(), any(), any()))
                    .thenReturn(filasDe(37));

            var resultado = service.crearRankingMensual(
                    UUID.randomUUID(), YearMonth.now().minusMonths(1));

            assertThat(resultado.getRanking())
                    .as("37 filas de entrada tienen que dar 37 posiciones, no 10")
                    .hasSize(37);
        }
    }

    @Nested
    @DisplayName("el recorte va al responder")
    class ElRecorteVaAlResponder {

        @Test
        @DisplayName("un limite grande devuelve tantas posiciones como haya, no 10")
        void unLimiteGrandeDevuelveTodas() {
            guardarRankingDe(50);

            RankingMesDTO conLimite = service.obtenerRankingConLimite(idRanking, 50);

            assertThat(conLimite.getRanking())
                    .as("antes devolvía 10 sin avisar que el ranking estaba truncado")
                    .hasSize(50);
        }

        @Test
        @DisplayName("un limite chico recorta de verdad")
        void unLimiteChicoRecorta() {
            guardarRankingDe(50);

            assertThat(service.obtenerRankingConLimite(idRanking, 3).getRanking())
                    .hasSize(3);
        }

        @Test
        @DisplayName("pedir más de lo que hay devuelve todo, sin error")
        void pedirMasDeLoQueHayDevuelveTodo() {
            guardarRankingDe(7);

            assertThat(service.obtenerRankingConLimite(idRanking, 500).getRanking())
                    .as("pedir de más no es un error: es un snapshot más chico que el pedido")
                    .hasSize(7);
        }

        @Test
        @DisplayName("el ranking completo también se puede pedir entero")
        void elRankingCompletoSePuedePedirEntero() {
            guardarRankingDe(30);

            assertThat(service.obtenerRanking(idRanking).getRanking()).hasSize(30);
        }

        @Test
        @DisplayName("el recorte no toca el ranking guardado: el siguiente pedido lo ve entero")
        void elRecorteNoTocaLoGuardado() {
            guardarRankingDe(30);

            service.obtenerRankingConLimite(idRanking, 3);

            assertThat(service.obtenerRanking(idRanking).getRanking())
                    .as("si el recorte vaciara el snapshot, el segundo pedido devolvería 3")
                    .hasSize(30);
        }
    }

    /** Guardo un ranking de {@code total} posiciones, igual que lo haría el service. */
    private void guardarRankingDe(int total) {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));

        for (int i = 1; i <= total; i++) {
            ranking.agregarPosicion(new Ranking(
                    ranking, UUID.randomUUID(), "Donante " + i, i, (long) (total - i)));
        }

        when(repoRankings.findById(idRanking)).thenReturn(Optional.of(ranking));
    }

    /** Las filas que devuelve la consulta del ranking: {@code [Perfil, total]}. */
    private List<Object[]> filasDe(int cantidad) {
        List<Object[]> filas = new ArrayList<>();

        for (int i = 1; i <= cantidad; i++) {
            Perfil perfil = new Perfil(UUID.randomUUID(), "Donante " + i);
            perfil.iniciarEn(new ar.edu.utn.frba.ddsi.incentivos.models.entities
                    .CategoriaPerfil.Categoria("Base", null, 1, List.of()));
            filas.add(new Object[]{perfil, (long) (cantidad - i)});
        }

        return filas;
    }
}
