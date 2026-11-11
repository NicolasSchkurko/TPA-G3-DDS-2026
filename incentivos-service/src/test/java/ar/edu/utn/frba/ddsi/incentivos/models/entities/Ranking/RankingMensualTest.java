package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RankingMensual: contenedor de posiciones")
class RankingMensualTest {

    @Test
    @DisplayName("agrega las posiciones y conserva el período")
    void agregaPosicionesYConservaElPeriodo() {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));
        Ranking posicion = new Ranking(ranking, UUID.randomUUID(), "Ana", 1, 9L);

        ranking.agregarPosicion(posicion);

        assertThat(ranking.getPosiciones()).containsExactly(posicion);
        assertThat(ranking.getPeriodo()).isEqualTo(YearMonth.of(2026, 2));
    }

    @Test
    @DisplayName("sin posiciones la colección queda vacía")
    void sinPosicionesQuedaVacia() {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));

        assertThat(ranking.getPosiciones()).isEmpty();
    }
}
