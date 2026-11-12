package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingFilaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingResumenDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilPublicoDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.ConflictoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("RankingService")
class RankingServiceTest {
        @Nested
        @DisplayName("Punto 32: no se publica el ranking de un período sin cerrar")
        class Periodo {


        private static final UUID ADMIN = UUID.randomUUID();

        private final RepositorioRankings repoRankings = mock(RepositorioRankings.class);
        private final RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        private final EntityManager entityManager = mock(EntityManager.class);
        private final RankingService service =
                new RankingService(repoRankings, repoPerfiles);

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            org.springframework.test.util.ReflectionTestUtils.setField(
                    service, "entityManager", entityManager);
        }

        @Nested
        @DisplayName("el efecto observable: el período no publica nada")
        class RechazaLoQueNoEstaCerrado {

            @Test
            @DisplayName("un mes futuro es un 400")
            void unMesFuturoEsUnError() {
                YearMonth futuro = YearMonth.now().plusYears(4);

                assertThatThrownBy(() -> service.crearRankingMensual(ADMIN, futuro))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining(futuro.toString());

                verify(repoRankings, never()).save(any());
            }

            @Test
            @DisplayName("el mes en curso es un 400: todavía no cerró y siempre sale vacío")
            void elMesEnCursoEsUnError() {
                YearMonth enCurso = YearMonth.now();

                assertThatThrownBy(() -> service.crearRankingMensual(ADMIN, enCurso))
                        .as("es el caso que se rompía solo, sin que nadie hiciera nada raro")
                        .isInstanceOf(IllegalArgumentException.class);

                verify(repoRankings, never()).save(any());
            }

            @Test
            @DisplayName("un período ya publicado es un 409")
            void unPeriodoYaPublicadoEsUnConflicto() {
                YearMonth cerrado = YearMonth.now().minusMonths(1);
                when(repoRankings.findByPeriodo(cerrado))
                        .thenReturn(Optional.of(mock(RankingMensual.class)));

                assertThatThrownBy(() -> service.crearRankingMensual(ADMIN, cerrado))
                        .isInstanceOf(ConflictoException.class)
                        .hasMessageContaining(cerrado.toString());

                verify(repoRankings, never()).saveAndFlush(any());
            }

            @Test
            @DisplayName("un mes ya cerrado se publica: es lo que hace el scheduler")
            void unMesCerradoSePublica() {
                YearMonth cerrado = YearMonth.now().minusMonths(1);
                when(repoRankings.findByPeriodo(cerrado)).thenReturn(Optional.empty());
                when(repoRankings.saveAndFlush(any())).thenAnswer(invoc -> invoc.getArgument(0));
                when(repoPerfiles.calcularRankingMensual(any(), any(), any()))
                        .thenReturn(new PageImpl<>(List.of()));
                when(repoRankings.paginaPosiciones(any(), any()))
                        .thenReturn(new PageImpl<>(List.of()));

                assertThat(service.crearRankingMensual(ADMIN, cerrado)).isNotNull();

                verify(repoRankings).saveAndFlush(any());
            }
        }

        @Nested
        @DisplayName("el scheduler también pasa por el control")
        class ElSchedulerTambien {

            @Test
            @DisplayName("el ranking mensual automático genera el mes anterior, no el actual")
            void elSchedulerGeneraElMesAnterior() {
                when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());
                when(repoRankings.saveAndFlush(any())).thenAnswer(invoc -> invoc.getArgument(0));
                when(repoPerfiles.calcularRankingMensual(any(), any(), any()))
                        .thenReturn(new PageImpl<>(List.of()));
                when(repoRankings.paginaPosiciones(any(), any()))
                        .thenReturn(new PageImpl<>(List.of()));

                assertThat(service.crearRankingMensualActual()).isNotNull();

                // El scheduler pasa por el mismo camino compartido que el endpoint.
                verify(repoRankings).saveAndFlush(any());
            }

            @Test
            @DisplayName("si el ranking ya existe, el scheduler no falla ni vuelve a guardar")
            void elSchedulerNoFallaSiElRankingYaExiste() {
                when(repoRankings.findByPeriodo(YearMonth.now().minusMonths(1)))
                        .thenReturn(Optional.of(mock(RankingMensual.class)));
                when(repoRankings.paginaPosiciones(any(), any()))
                        .thenReturn(new PageImpl<>(List.of()));

                assertThat(service.crearRankingMensualActual()).isNotNull();

                verify(repoRankings, never()).saveAndFlush(any());
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
                when(repoRankings.paginaPosiciones(any(), any()))
                        .thenReturn(new PageImpl<>(List.of()));

                assertThat(service.obtenerRankingActual(PageRequest.of(0, 10))).isNotNull();

                verify(repoRankings).findFirstByPeriodoLessThanOrderByPeriodoDesc(YearMonth.now());
            }

            @Test
            @DisplayName("el método viejo de 'el más alto' ya no existe: no es el que se usa")
            void elMetodoViejoNoExiste() {
                // findFirstByOrderByPeriodoDesc() era el que rompía el ranking actual con un futuro.
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

        @Nested
        @DisplayName("Ranking: generación por lotes y lecturas paginadas")
        class Snapshot {


        private final RepositorioRankings repoRankings = mock(RepositorioRankings.class);
        private final RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        private final EntityManager entityManager = mock(EntityManager.class);
        private final RankingService service =
                new RankingService(repoRankings, repoPerfiles);

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            org.springframework.test.util.ReflectionTestUtils.setField(
                    service, "entityManager", entityManager);
        }

        private final UUID idRanking = UUID.randomUUID();

        @Test
        @DisplayName("la generación consulta el ranking por páginas, no de una sola vez")
        void laGeneracionConsultaPorPaginas() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());
            when(repoPerfiles.calcularRankingMensual(any(), any(), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));
            when(repoRankings.paginaPosiciones(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.crearRankingMensual(UUID.randomUUID(), YearMonth.now().minusMonths(1));

            assertThat(captor.getValue().isPaged()).isTrue();
            assertThat(captor.getValue().getPageSize()).isEqualTo(500);
        }

        @Test
        @DisplayName("persiste todas las filas del período, aunque la respuesta venga paginada")
        void persisteTodasLasFilasDelPeriodo() {
            when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());
            when(repoPerfiles.calcularRankingMensual(any(), any(), any()))
                    .thenReturn(new PageImpl<>(filasDe(37)));
            when(repoRankings.paginaPosiciones(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.crearRankingMensual(UUID.randomUUID(), YearMonth.now().minusMonths(1));

            ArgumentCaptor<Ranking> captor = ArgumentCaptor.forClass(Ranking.class);
            verify(entityManager, times(37)).persist(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(Ranking::getPuesto)
                    .containsExactlyElementsOf(posicionesDelUnoAl(37));
        }

        @Test
        @DisplayName("los empates comparten puesto aunque crucen una página")
        void losEmpatesCruzanLaPagina() {
            List<RankingFilaDTO> primeraPagina = new ArrayList<>();
            for (int i = 0; i < 499; i++) {
                primeraPagina.add(new RankingFilaDTO(UUID.randomUUID(), "Donante " + i, 10L));
            }
            primeraPagina.add(new RankingFilaDTO(UUID.randomUUID(), "Donante 499", 5L));
            List<RankingFilaDTO> segundaPagina = List.of(
                    new RankingFilaDTO(UUID.randomUUID(), "Donante 500", 5L));

            when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());
            when(repoPerfiles.calcularRankingMensual(any(), any(), any()))
                    .thenReturn(new PageImpl<>(primeraPagina, PageRequest.of(0, 500), 501))
                    .thenReturn(new PageImpl<>(segundaPagina, PageRequest.of(1, 500), 501));
            when(repoRankings.paginaPosiciones(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.crearRankingMensual(UUID.randomUUID(), YearMonth.now().minusMonths(1));

            ArgumentCaptor<Ranking> captor = ArgumentCaptor.forClass(Ranking.class);
            verify(entityManager, times(501)).persist(captor.capture());
            List<Ranking> persistidos = captor.getAllValues();

            assertThat(persistidos.get(498).getPuesto()).isEqualTo(1);
            assertThat(persistidos.get(499).getPuesto()).isEqualTo(500);
            assertThat(persistidos.get(500).getPuesto()).isEqualTo(500);
            verify(entityManager, times(2)).clear();
        }

        @Test
        @DisplayName("el límite del top se aplica en la consulta de posiciones")
        void elLimiteDelTopSeAplicaEnLaConsulta() {
            RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));
            when(repoRankings.findById(idRanking)).thenReturn(Optional.of(ranking));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(repoRankings.paginaPosiciones(eq(idRanking), captor.capture()))
                    .thenReturn(new PageImpl<>(posicionesDe(ranking, 3)));

            RankingMesDTO resultado = service.obtenerRankingConLimite(idRanking, 3);

            assertThat(resultado.getRanking().getContent()).hasSize(3);
            assertThat(captor.getValue().getPageSize()).isEqualTo(3);
            assertThat(captor.getValue().getOffset()).isZero();
        }

        @Test
        @DisplayName("un límite mayor al máximo se acota para no cargar todo")
        void limiteMayorAlMaximoSeAcota() {
            RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 2));
            when(repoRankings.findById(idRanking)).thenReturn(Optional.of(ranking));
            when(repoRankings.paginaPosiciones(eq(idRanking), any()))
                    .thenAnswer(invocacion -> {
                        Pageable pageable = invocacion.getArgument(1);
                        return new PageImpl<>(posicionesDe(ranking, pageable.getPageSize()));
                    });

            RankingMesDTO resultado = service.obtenerRankingConLimite(idRanking, 500);

            assertThat(resultado.getRanking().getContent()).hasSize(RankingService.LIMITE_MAXIMO);
        }

        @Test
        @DisplayName("el detalle también se pagina y no carga la colección del ranking")
        void elDetalleSePaginayNoUsaLaColeccion() {
            RankingMensual ranking = mock(RankingMensual.class);
            when(ranking.getIdRanking()).thenReturn(idRanking);
            when(ranking.getPeriodo()).thenReturn(YearMonth.of(2026, 2));
            when(repoRankings.findById(idRanking)).thenReturn(Optional.of(ranking));

            when(repoRankings.paginaPosiciones(eq(idRanking), any()))
                    .thenAnswer(invocacion -> {
                        Pageable pageable = invocacion.getArgument(1);
                        return new PageImpl<>(posicionesDe(ranking, pageable.getPageSize()));
                    });

            RankingMesDTO resultado = service.obtenerRanking(idRanking, PageRequest.of(0, 30));

            assertThat(resultado.getRanking().getContent()).hasSize(30);
            verify(ranking, never()).getPosiciones();
        }

        @Test
        @DisplayName("el historial devuelve encabezados sin cargar posiciones")
        void elHistorialDevuelveEncabezadosSinPosiciones() {
            RankingMensual ranking = mock(RankingMensual.class);
            when(ranking.getIdRanking()).thenReturn(idRanking);
            when(ranking.getPeriodo()).thenReturn(YearMonth.of(2026, 3));

            Pageable pageable = PageRequest.of(0, 10);
            when(repoRankings.findAllByOrderByPeriodoDesc(pageable))
                    .thenReturn(new PageImpl<>(List.of(ranking), pageable, 1));

            Page<RankingResumenDTO> resultado = service.obtenerHistorialRankings(pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
            assertThat(resultado.getContent().get(0).periodo()).isEqualTo(YearMonth.of(2026, 3));
            verify(ranking, never()).getPosiciones();
        }

        private List<RankingFilaDTO> filasDe(int cantidad) {
            List<RankingFilaDTO> filas = new ArrayList<>();
            for (int i = 1; i <= cantidad; i++) {
                filas.add(new RankingFilaDTO(
                        UUID.randomUUID(), "Donante " + i, (long) (cantidad - i)));
            }
            return filas;
        }

        private List<Integer> posicionesDelUnoAl(int cantidad) {
            List<Integer> posiciones = new ArrayList<>();
            for (int i = 1; i <= cantidad; i++) {
                posiciones.add(i);
            }
            return posiciones;
        }

        private List<Ranking> posicionesDe(RankingMensual ranking, int cantidad) {
            List<Ranking> posiciones = new ArrayList<>();
            for (int i = 1; i <= cantidad; i++) {
                posiciones.add(new Ranking(
                        ranking, UUID.randomUUID(), "Donante " + i, i, (long) (cantidad - i)));
            }
            return posiciones;
        }
        }

        

}
