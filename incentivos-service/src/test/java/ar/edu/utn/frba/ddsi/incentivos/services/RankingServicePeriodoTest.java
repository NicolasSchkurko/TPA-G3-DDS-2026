package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import java.lang.reflect.Method;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * No se puede publicar el ranking de un mes que todavía no cerró (punto 32).
 *
 * <p>El daño no era el ranking futuro en sí, sino lo que hacía con el resto del servicio.
 * "El ranking actual" se resolvía con el período más alto existente
 * ({@code findFirstByOrderByPeriodoDesc()}), así que un solo {@code POST
 * /api/rankings {"periodo":"2030-01"}} dejaba el servicio en este estado:
 *
 * <ul>
 *   <li>{@code GET /api/rankings/actual} devolvía la lista vacía, porque en 2030 no hay
 *       nadie.</li>
 *   <li>{@code GET /api/rankings/{id}/puestoRanking} respondía <b>404 para todos los
 *       usuarios</b>, aunque el ranking verdadero estuviera ahí.</li>
 * </ul>
 *
 * <p>Y quedaba así hasta que alguien se diera cuenta y borrara el ranking futuro a mano.
 * El mes en curso pasaba lo mismo, con la diferencia de que se rompía solo: siempre sale
 * vacío, porque el mes no terminó.
 */
@DisplayName("Punto 32: no se publica el ranking de un período sin cerrar")
class RankingServicePeriodoTest {

    private static final UUID ADMIN = UUID.randomUUID();

    private final RepositorioRankings repoRankings = mock(RepositorioRankings.class);
    private final RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
    private final ValidadorAdmin validador = mock(ValidadorAdmin.class);
    private final RankingService service =
            new RankingService(repoRankings, repoPerfiles, validador);

    private void adminValido() {
        org.mockito.Mockito.doNothing().when(validador).verificarPermisos(any());
    }

    @Nested
    @DisplayName("el efecto observable: el período no publica nada")
    class RechazaLoQueNoEstaCerrado {

        @Test
        @DisplayName("un mes futuro es un 400")
        void unMesFuturoEsUnError() {
            adminValido();
            YearMonth futuro = YearMonth.now().plusYears(4);

            assertThatThrownBy(() -> service.crearRankingMensual(ADMIN, futuro))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(futuro.toString());

            verify(repoRankings, never()).save(any());
        }

        @Test
        @DisplayName("el mes en curso es un 400: todavía no cerró y siempre sale vacío")
        void elMesEnCursoEsUnError() {
            adminValido();
            YearMonth enCurso = YearMonth.now();

            assertThatThrownBy(() -> service.crearRankingMensual(ADMIN, enCurso))
                    .as("es el caso que se rompía solo, sin que nadie hiciera nada raro")
                    .isInstanceOf(IllegalArgumentException.class);

            verify(repoRankings, never()).save(any());
        }

        @Test
        @DisplayName("un mes ya cerrado se publica: es lo que hace el scheduler")
        void unMesCerradoSePublica() {
            adminValido();
            YearMonth cerrado = YearMonth.now().minusMonths(1);
            when(repoRankings.findByPeriodo(cerrado)).thenReturn(Optional.empty());
            when(repoRankings.save(any())).thenAnswer(invoc -> invoc.getArgument(0));
            when(repoPerfiles.calcularRankingMensual(any(), any(), any()))
                    .thenReturn(java.util.List.of());

            assertThat(service.crearRankingMensual(ADMIN, cerrado)).isNotNull();

            verify(repoRankings).save(any());
        }
    }

    @Nested
    @DisplayName("el scheduler también pasa por el control")
    class ElSchedulerTambien {

        @Test
        @DisplayName("el ranking mensual automático genera el mes anterior, no el actual")
        void elSchedulerGeneraElMesAnterior() {
            when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());
            when(repoRankings.save(any())).thenAnswer(invoc -> invoc.getArgument(0));
            when(repoPerfiles.calcularRankingMensual(any(), any(), any()))
                    .thenReturn(java.util.List.of());

            assertThat(service.crearRankingMensualActual()).isNotNull();

            // El scheduler pasa por generarYGuardar, que es el mismo camino que el endpoint.
            // Si el control hubiera quedado solo en el endpoint, esta llamada seguiría
            // funcionando por casualidad; el test la ata al camino compartido.
            verify(repoRankings).save(any());
        }
    }

    @Nested
    @DisplayName("el ranking actual ignora los períodos sin cerrar, estén o no")
    class ElActualIgnoraLoQueNoEstaCerrado {

        @Test
        @DisplayName("'el actual' se busca con el filtro por período, no con el más alto existente")
        void elActualSeBuscaConFiltro() {
            var rankingCerrado = mock(
                    ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual.class);
            when(repoRankings.findFirstByPeriodoLessThanOrderByPeriodoDesc(any()))
                    .thenReturn(Optional.of(rankingCerrado));
            when(rankingCerrado.getPosiciones()).thenReturn(java.util.List.of());

            assertThat(service.obtenerRankingActual()).isNotNull();

            verify(repoRankings).findFirstByPeriodoLessThanOrderByPeriodoDesc(YearMonth.now());
        }

        @Test
        @DisplayName("el método viejo de 'el más alto' ya no existe: no es el que se usa")
        void elMetodoViejoNoExiste() {
            // findFirstByOrderByPeriodoDesc() era el que rompía el ranking actual con un
            // ranking futuro. Si alguien lo vuelve a agregar, este test no lo va a agarrar,
            // pero al menos deja de estar disponible para que alguien lo use por costumbre.
            boolean existe = false;
            for (Method metodo : RepositorioRankings.class.getDeclaredMethods()) {
                if (metodo.getName().equals("findFirstByOrderByPeriodoDesc")) {
                    existe = true;
                }
            }

            assertThat(existe)
                    .as("el 'ranking actual' tiene que ser el último mes cerrado, no el más alto")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("el filtro del ranking mensual es un rango, no funciones sobre la fecha")
    class ElFiltroEsSargable {

        @Test
        @DisplayName("calcularRankingMensual recibe instantes, no mes y año")
        void calcularRankingMensualRecibeInstantes() throws Exception {
            Method metodo = RepositorioPerfiles.class
                    .getMethod("calcularRankingMensual", java.time.LocalDateTime.class,
                            java.time.LocalDateTime.class,
                            org.springframework.data.domain.Pageable.class);

            var anotacion = metodo.getAnnotation(
                    org.springframework.data.jpa.repository.Query.class);

            assertThat(anotacion).isNotNull();
            assertThat(anotacion.value())
                    .as("""
                    MONTH(fecha) = n AND YEAR(fecha) = a no es sargable: la base tiene que
                    evaluar la función sobre cada fila de la tabla antes de comparar, así que
                    el índice de la fecha no sirve y cada ranking mensual era un recorrido
                    completo.
                    """)
                    .doesNotContain("MONTH(")
                    .doesNotContain("YEAR(")
                    .contains("fechaObtencion >= :inicio")
                    .contains("fechaObtencion < :fin");
        }
    }
}
