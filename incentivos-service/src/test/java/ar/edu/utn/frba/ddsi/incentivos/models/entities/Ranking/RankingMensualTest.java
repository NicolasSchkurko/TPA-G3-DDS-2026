package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RankingMensual: calculo de posiciones")
class RankingMensualTest {

    private static Object[] fila(String nombre, long misiones) {
        Perfil perfil = new Perfil(UUID.randomUUID(), nombre);
        return new Object[]{perfil, misiones};
    }

    private static List<Ranking> puestos(RankingMensual ranking) {
        return ranking.getPosiciones().stream()
                .sorted((a, b) -> a.getPuesto().compareTo(b.getPuesto()))
                .toList();
    }

    @Test
    @DisplayName("asigna puestos correlativos y respeta el orden recibido")
    void asignaPuestosCorrelativos() {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));

        ranking.calcularYAgregarPosiciones(List.of(
                fila("Ana", 9L),
                fila("Beto", 7L),
                fila("Caro", 4L)
        ));

        List<Ranking> posiciones = puestos(ranking);
        assertThat(posiciones).extracting(Ranking::getPuesto).containsExactly(1, 2, 3);
        assertThat(posiciones).extracting(Ranking::getNombreUsuario)
                .containsExactly("Ana", "Beto", "Caro");
        assertThat(posiciones).extracting(Ranking::getMisionesCumplidas)
                .containsExactly(9L, 7L, 4L);
    }

    @Test
    @DisplayName("los perfiles empatados comparten el mismo puesto")
    void losEmpatesCompartenPuesto() {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));

        ranking.calcularYAgregarPosiciones(List.of(
                fila("Ana", 9L),
                fila("Beto", 4L),
                fila("Caro", 4L),
                fila("Dani", 1L)
        ));

        assertThat(ranking.getPosiciones())
                .extracting(Ranking::getPuesto).containsExactly(1, 2, 2, 4);
    }

    @Test
    @DisplayName("con todos empatados arrancan en el puesto uno")
    void todosEmpatadosArrancanEnUno() {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));

        ranking.calcularYAgregarPosiciones(List.of(
                fila("Ana", 5L),
                fila("Beto", 5L)
        ));

        assertThat(ranking.getPosiciones()).extracting(Ranking::getPuesto).containsExactly(1, 1);
    }

    @Test
    @DisplayName("sin datos no genera posiciones")
    void sinDatosNoGeneraPosiciones() {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));

        ranking.calcularYAgregarPosiciones(List.of());

        assertThat(ranking.getPosiciones()).isEmpty();
    }

    @Test
    @DisplayName("respeta el limite recibido desde el repositorio")
    void respetaElLimiteRecibido() {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));

        ranking.calcularYAgregarPosiciones(List.of(
                fila("Ana", 9L),
                fila("Beto", 7L),
                fila("Caro", 4L)
        ));

        assertThat(ranking.getPosiciones()).hasSize(3);
        assertThat(ranking.getPeriodo()).isEqualTo(YearMonth.of(2026, 2));
    }
}